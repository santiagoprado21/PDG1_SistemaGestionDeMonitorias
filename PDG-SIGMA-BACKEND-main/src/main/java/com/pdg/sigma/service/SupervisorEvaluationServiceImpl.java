package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupervisorEvaluationServiceImpl implements SupervisorEvaluationService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 7;
    private static final String PERIOD_REGEX = "^\\d{4}-[12]$";

    @Autowired
    private SupervisorEvaluationRepository supervisorEvaluationRepository;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private ProfessorSurveySemesterConfigRepository professorSurveySemesterConfigRepository;

    @Autowired
    private ProfessorSurveySemesterQuestionRepository professorSurveySemesterQuestionRepository;

    @Autowired
    private SupervisorEvaluationAnswerRepository supervisorEvaluationAnswerRepository;

    @Override
    @Transactional
    public SupervisorEvaluationResponse createEvaluation(String monitorIdentifier, SupervisorEvaluationRequest request) throws Exception {
        String resolvedMonitorId = resolveMonitorIdentifier(monitorIdentifier, request);
        validateRequest(resolvedMonitorId, request);

        Monitor monitor = resolveMonitor(resolvedMonitorId);
        Monitoring monitoring = monitoringRepository.findById(request.getMonitoringId())
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));

        ensureMonitorAssignment(monitoring, monitor);

        supervisorEvaluationRepository.findByMonitoringIdAndMonitorCode(monitoring.getId(), monitor.getCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Ya existe una evaluación registrada para esta monitoría");
                });

        Professor professor = Optional.ofNullable(monitoring.getProfessor())
                .orElseThrow(() -> new Exception("Profesor supervisor no encontrado"));

        MonitoringMonitor monitoringMonitor = monitoringMonitorRepository
                .findByMonitoringIdAndMonitorCode(monitoring.getId(), monitor.getCode())
                .orElse(null);

        String semester = normalizePeriod(monitoring.getSemester());
        if (semester == null) {
            semester = resolveCurrentPeriod();
        }
        EvaluationAnswersContext dynamicAnswers = resolveDynamicAnswers(request, semester);

        SupervisorEvaluation evaluation = new SupervisorEvaluation();
        evaluation.setMonitoring(monitoring);
        evaluation.setMonitor(monitor);
        evaluation.setProfessor(professor);
        evaluation.setMonitoringMonitor(monitoringMonitor);
        evaluation.setSemester(semester);
        evaluation.setSubmittedBy(resolveSubmittedBy(monitor));

        if (dynamicAnswers != null) {
            int[] legacyScores = dynamicAnswers.getLegacyScores();
            evaluation.applyScores(
                    legacyScores[0],
                    legacyScores[1],
                    legacyScores[2],
                    legacyScores[3],
                    legacyScores[4],
                    legacyScores[5],
                    legacyScores[6],
                    legacyScores[7],
                    request.getStrengthsComments(),
                    request.getImprovementComments()
            );
            evaluation.setTotalScore(dynamicAnswers.getAverageScore());
            evaluation.setPerformanceLevel(resolvePerformanceLevel(dynamicAnswers.getAverageScore()));
        } else {
            validateLegacyScores(request);
            evaluation.applyScores(
                    request.getGuidanceClarity(),
                    request.getRoleExpectations(),
                    request.getAvailabilityDisposition(),
                    request.getSupportTimeliness(),
                    request.getFeedbackConstructive(),
                    request.getFeedbackFairness(),
                    request.getRespectfulTreatment(),
                    request.getTrustEnvironment(),
                    request.getStrengthsComments(),
                    request.getImprovementComments()
            );
        }

        SupervisorEvaluation saved = supervisorEvaluationRepository.save(evaluation);

        if (dynamicAnswers != null) {
            List<SupervisorEvaluationAnswer> answerRows = new ArrayList<>();
            for (ResolvedAnswer answer : dynamicAnswers.getOrderedAnswers()) {
                SupervisorEvaluationAnswer row = new SupervisorEvaluationAnswer();
                row.setEvaluation(saved);
                row.setQuestion(answer.getQuestion());
                row.setDisplayOrder(answer.getDisplayOrder());
                row.setScore(answer.getScore());
                answerRows.add(row);
            }
            supervisorEvaluationAnswerRepository.saveAll(answerRows);
            saved.setAnswers(answerRows);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorEvaluationStatusDTO> getAssignmentsForMonitor(String monitorIdentifier) throws Exception {
        Monitor monitor = resolveMonitor(monitorIdentifier);

        Map<Long, SupervisorEvaluation> evaluationMap = supervisorEvaluationRepository
                .findByMonitorCodeOrderByCreatedAtDesc(monitor.getCode())
                .stream()
                .filter(evaluation -> evaluation.getMonitoring() != null && evaluation.getMonitoring().getId() != null)
                .collect(Collectors.toMap(
                        evaluation -> evaluation.getMonitoring().getId(),
                        evaluation -> evaluation,
                        (first, second) -> first
                ));

        Set<Long> processedMonitoringIds = new HashSet<>();
        List<SupervisorEvaluationStatusDTO> assignments = new ArrayList<>();

        List<MonitoringMonitor> relations = monitoringMonitorRepository.findByMonitor(monitor);
        if (relations != null) {
            for (MonitoringMonitor relation : relations) {
                if (relation == null || relation.getMonitoring() == null) {
                    continue;
                }
                if (!isEligibleStatus(relation.getEstadoSeleccion())) {
                    continue;
                }
                Monitoring monitoring = relation.getMonitoring();
                if (monitoring.getId() == null || !processedMonitoringIds.add(monitoring.getId())) {
                    continue;
                }
                SupervisorEvaluation evaluation = evaluationMap.get(monitoring.getId());
                assignments.add(buildStatusDto(monitoring, relation, evaluation));
            }
        }

        List<Monitoring> directMonitorings = monitoringRepository.findByAssignedMonitor(monitor);
        if (directMonitorings != null) {
            for (Monitoring monitoring : directMonitorings) {
                if (monitoring == null || monitoring.getId() == null || !processedMonitoringIds.add(monitoring.getId())) {
                    continue;
                }
                SupervisorEvaluation evaluation = evaluationMap.get(monitoring.getId());
                assignments.add(buildStatusDto(monitoring, null, evaluation));
            }
        }

        assignments.sort(Comparator
                .comparing(SupervisorEvaluationStatusDTO::isEvaluated)
                .thenComparing(SupervisorEvaluationStatusDTO::getMonitoringName, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        return assignments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorEvaluationResponse> getEvaluationsForCoordinator() throws Exception {
        return supervisorEvaluationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorEvaluationResponse> getEvaluationsByProfessor(String professorId) throws Exception {
        if (professorId == null || professorId.trim().isEmpty()) {
            throw new Exception("Debe indicar el profesor supervisor");
        }
        if (!professorRepository.existsById(professorId)) {
            throw new Exception("Profesor supervisor no encontrado");
        }
        return supervisorEvaluationRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorEvaluationResponse> getEvaluationsByMonitor(String monitorIdentifier) throws Exception {
        Monitor monitor = resolveMonitor(monitorIdentifier);
        return supervisorEvaluationRepository.findByMonitorCodeOrderByCreatedAtDesc(monitor.getCode())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupervisorEvaluationResponse> getEvaluation(Long evaluationId) {
        return supervisorEvaluationRepository.findById(evaluationId)
                .map(this::toResponse);
    }

    private void validateRequest(String monitorIdentifier, SupervisorEvaluationRequest request) throws Exception {
        if (monitorIdentifier == null || monitorIdentifier.trim().isEmpty()) {
            throw new Exception("Debe indicar el monitor que envía la evaluación");
        }
        if (request.getMonitoringId() == null) {
            throw new Exception("Debe especificar la monitoría a evaluar");
        }
    }

    private void validateLegacyScores(SupervisorEvaluationRequest request) throws Exception {
        ensureScoreInRange("Claridad de la orientación", request.getGuidanceClarity());
        ensureScoreInRange("Expectativas del rol", request.getRoleExpectations());
        ensureScoreInRange("Disponibilidad para atender dudas", request.getAvailabilityDisposition());
        ensureScoreInRange("Acompañamiento oportuno", request.getSupportTimeliness());
        ensureScoreInRange("Retroalimentación constructiva", request.getFeedbackConstructive());
        ensureScoreInRange("Evaluación justa", request.getFeedbackFairness());
        ensureScoreInRange("Trato respetuoso", request.getRespectfulTreatment());
        ensureScoreInRange("Ambiente de confianza", request.getTrustEnvironment());
    }

    private EvaluationAnswersContext resolveDynamicAnswers(SupervisorEvaluationRequest request, String semester) throws Exception {
        List<SupervisorEvaluationAnswerRequestDTO> rawAnswers = Optional.ofNullable(request.getAnswers())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (rawAnswers.isEmpty()) {
            return null;
        }

        Optional<ProfessorSurveySemesterConfig> configOpt = Optional.empty();
        if (semester != null && !semester.isBlank()) {
            configOpt = professorSurveySemesterConfigRepository.findBySemester(semester.trim());
        }
        if (configOpt.isEmpty()) {
            configOpt = professorSurveySemesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc();
        }

        ProfessorSurveySemesterConfig config = configOpt
                .orElseThrow(() -> new Exception("No existe una configuración activa para la encuesta de profesores"));

        List<ProfessorSurveySemesterQuestion> configuredQuestions = professorSurveySemesterQuestionRepository
                .findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(config.getId())
                .stream()
                .filter(entry -> entry.getQuestion() != null && entry.getQuestion().getId() != null)
                .collect(Collectors.toList());

        if (configuredQuestions.isEmpty()) {
            throw new Exception("No hay preguntas activas configuradas para el periodo " + config.getSemester());
        }

        Map<Long, ProfessorSurveySemesterQuestion> configuredById = configuredQuestions.stream()
                .collect(Collectors.toMap(entry -> entry.getQuestion().getId(), entry -> entry));

        Map<Long, Integer> scoresByQuestionId = new HashMap<>();
        for (SupervisorEvaluationAnswerRequestDTO answer : rawAnswers) {
            if (answer.getQuestionId() == null) {
                throw new Exception("Cada respuesta debe indicar la pregunta");
            }
            if (scoresByQuestionId.containsKey(answer.getQuestionId())) {
                throw new Exception("No se permiten respuestas duplicadas para la misma pregunta");
            }
            ensureScoreInRange("Pregunta " + answer.getQuestionId(), answer.getScore());
            if (!configuredById.containsKey(answer.getQuestionId())) {
                throw new Exception("La pregunta " + answer.getQuestionId() + " no hace parte de la configuración activa");
            }
            scoresByQuestionId.put(answer.getQuestionId(), answer.getScore());
        }

        if (scoresByQuestionId.size() != configuredQuestions.size()) {
            throw new Exception("Debes responder todas las preguntas activas del periodo");
        }

        List<ResolvedAnswer> orderedAnswers = new ArrayList<>();
        for (ProfessorSurveySemesterQuestion configured : configuredQuestions) {
            Long questionId = configured.getQuestion().getId();
            Integer score = scoresByQuestionId.get(questionId);
            if (score == null) {
                throw new Exception("Falta la respuesta para la pregunta " + questionId);
            }
            orderedAnswers.add(new ResolvedAnswer(configured.getQuestion(), configured.getDisplayOrder(), score));
        }

        double averageScore = orderedAnswers.stream()
                .mapToInt(ResolvedAnswer::getScore)
                .average()
                .orElse(0.0);
        averageScore = Math.round(averageScore * 100.0) / 100.0;

        int roundedAverage = (int) Math.round(averageScore);
        if (roundedAverage < MIN_SCORE) {
            roundedAverage = MIN_SCORE;
        }
        if (roundedAverage > MAX_SCORE) {
            roundedAverage = MAX_SCORE;
        }

        int[] legacyScores = new int[]{roundedAverage, roundedAverage, roundedAverage, roundedAverage,
                roundedAverage, roundedAverage, roundedAverage, roundedAverage};

        for (int i = 0; i < Math.min(8, orderedAnswers.size()); i++) {
            legacyScores[i] = orderedAnswers.get(i).getScore();
        }

        return new EvaluationAnswersContext(orderedAnswers, averageScore, legacyScores);
    }

    private void ensureScoreInRange(String label, Integer value) throws Exception {
        if (value == null) {
            throw new Exception("Debe proporcionar una calificación para " + label);
        }
        if (value < MIN_SCORE || value > MAX_SCORE) {
            throw new Exception(label + " debe estar entre " + MIN_SCORE + " y " + MAX_SCORE);
        }
    }

    private String resolvePerformanceLevel(double score) {
        if (score >= 6.0) {
            return "EXCELENTE";
        }
        if (score >= 5.0) {
            return "DESTACADO";
        }
        if (score >= 4.0) {
            return "ADECUADO";
        }
        return "EN_RIESGO";
    }

    private String resolveMonitorIdentifier(String explicit, SupervisorEvaluationRequest request) {
        if (explicit != null && !explicit.trim().isEmpty()) {
            return explicit.trim();
        }
        if (request.getMonitorIdentifier() != null && !request.getMonitorIdentifier().trim().isEmpty()) {
            return request.getMonitorIdentifier().trim();
        }
        return null;
    }

    private Monitor resolveMonitor(String monitorIdentifier) throws Exception {
        if (monitorIdentifier == null || monitorIdentifier.trim().isEmpty()) {
            throw new Exception("Debe indicar el monitor");
        }
        String trimmed = monitorIdentifier.trim();

        Optional<Monitor> monitor = monitorRepository.findById(trimmed);
        if (monitor.isPresent()) {
            return monitor.get();
        }
        monitor = monitorRepository.findByCode(trimmed);
        if (monitor.isPresent()) {
            return monitor.get();
        }
        monitor = monitorRepository.findByIdMonitor(trimmed);
        if (monitor.isPresent()) {
            return monitor.get();
        }
        throw new Exception("Monitor no encontrado");
    }

    private void ensureMonitorAssignment(Monitoring monitoring, Monitor monitor) throws Exception {
        if (monitoring == null || monitor == null) {
            throw new Exception("Monitoría o monitor inválido");
        }
        Monitor assigned = monitoring.getAssignedMonitor();
        if (assigned != null && (assigned.getCode().equalsIgnoreCase(monitor.getCode())
                || (assigned.getIdMonitor() != null && assigned.getIdMonitor().equalsIgnoreCase(monitor.getIdMonitor())))) {
            return;
        }

        Optional<MonitoringMonitor> relation = monitoringMonitorRepository
                .findByMonitoringIdAndMonitorCode(monitoring.getId(), monitor.getCode());
        if (relation.isPresent() && isEligibleStatus(relation.get().getEstadoSeleccion())) {
            return;
        }

        throw new Exception("El monitor no está asignado a la monitoría indicada");
    }

    private boolean isEligibleStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return "seleccionado".equals(normalized)
                || "aprobado".equals(normalized)
                || "asignado".equals(normalized);
    }

    private SupervisorEvaluationStatusDTO buildStatusDto(Monitoring monitoring, MonitoringMonitor relation, SupervisorEvaluation evaluation) {
        SupervisorEvaluationStatusDTO dto = new SupervisorEvaluationStatusDTO();
        dto.setMonitoringId(monitoring.getId());
        dto.setMonitoringMonitorId(relation != null ? relation.getId() : null);
        dto.setEvaluationId(evaluation != null ? evaluation.getId() : null);

        String courseName = monitoring.getCourse() != null ? monitoring.getCourse().getName() : null;
        String programName = monitoring.getProgram() != null ? monitoring.getProgram().getName() : null;
        String semester = normalizePeriod(monitoring.getSemester());
        String monitoringName = buildMonitoringName(courseName, semester);

        dto.setMonitoringName(monitoringName);
        dto.setCourseName(courseName);
        dto.setProgramName(programName);
        dto.setSemester(semester);

        Professor professor = monitoring.getProfessor();
        dto.setProfessorId(professor != null ? professor.getId() : null);
        dto.setProfessorName(professor != null ? professor.getName() : null);

        if (evaluation != null) {
            dto.setEvaluated(true);
            dto.setStatus("ENVIADA");
            dto.setSubmittedAt(evaluation.getCreatedAt());
        } else {
            dto.setEvaluated(false);
            dto.setStatus("PENDIENTE");
        }
        return dto;
    }

    private SupervisorEvaluationResponse toResponse(SupervisorEvaluation evaluation) {
        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(evaluation.getId());
        response.setMonitoringId(evaluation.getMonitoring() != null ? evaluation.getMonitoring().getId() : null);
        response.setMonitoringMonitorId(evaluation.getMonitoringMonitor() != null ? evaluation.getMonitoringMonitor().getId() : null);

        Monitoring monitoring = evaluation.getMonitoring();
        if (monitoring != null) {
            String courseName = monitoring.getCourse() != null ? monitoring.getCourse().getName() : null;
            String programName = monitoring.getProgram() != null ? monitoring.getProgram().getName() : null;
            String normalizedPeriod = normalizePeriod(monitoring.getSemester());
            response.setMonitoringName(buildMonitoringName(courseName, normalizedPeriod));
            response.setCourseName(courseName);
            response.setProgramName(programName);
            response.setSemester(normalizedPeriod);
            response.setProfessorId(monitoring.getProfessor() != null ? monitoring.getProfessor().getId() : null);
            response.setProfessorName(monitoring.getProfessor() != null ? monitoring.getProfessor().getName() : null);
        }

        Monitor monitor = evaluation.getMonitor();
        if (monitor != null) {
            response.setMonitorCode(monitor.getCode());
            response.setMonitorIdentifier(monitor.getIdMonitor());
            response.setMonitorFullName(buildMonitorFullName(monitor));
            response.setMonitorEmail(monitor.getEmail());
        }

        response.setGuidanceClarity(evaluation.getGuidanceClarity());
        response.setRoleExpectations(evaluation.getRoleExpectations());
        response.setAvailabilityDisposition(evaluation.getAvailabilityDisposition());
        response.setSupportTimeliness(evaluation.getSupportTimeliness());
        response.setFeedbackConstructive(evaluation.getFeedbackConstructive());
        response.setFeedbackFairness(evaluation.getFeedbackFairness());
        response.setRespectfulTreatment(evaluation.getRespectfulTreatment());
        response.setTrustEnvironment(evaluation.getTrustEnvironment());

        response.setTotalScore(evaluation.getTotalScore());
        response.setPerformanceLevel(evaluation.getPerformanceLevel());
        response.setStrengthsComments(evaluation.getStrengthsComments());
        response.setImprovementComments(evaluation.getImprovementComments());
        response.setSubmittedBy(evaluation.getSubmittedBy());

        List<SupervisorEvaluationAnswerResponseDTO> answers = Optional.ofNullable(evaluation.getAnswers())
            .orElse(List.of())
            .stream()
            .sorted(Comparator.comparingInt(SupervisorEvaluationAnswer::getDisplayOrder))
                .map(this::toAnswerResponse)
                .collect(Collectors.toList());
        response.setAnswers(answers);

        response.setCreatedAt(evaluation.getCreatedAt());
        response.setUpdatedAt(evaluation.getUpdatedAt());
        return response;
    }

    private SupervisorEvaluationAnswerResponseDTO toAnswerResponse(SupervisorEvaluationAnswer answer) {
        SupervisorEvaluationAnswerResponseDTO dto = new SupervisorEvaluationAnswerResponseDTO();
        dto.setQuestionId(answer.getQuestion() != null ? answer.getQuestion().getId() : null);
        dto.setQuestionKey(answer.getQuestion() != null ? answer.getQuestion().getQuestionKey() : null);
        dto.setStatement(answer.getQuestion() != null ? answer.getQuestion().getStatement() : null);
        dto.setCategory(answer.getQuestion() != null ? answer.getQuestion().getCategory() : null);
        dto.setDisplayOrder(answer.getDisplayOrder());
        dto.setScore(answer.getScore());
        return dto;
    }

    private String resolveSubmittedBy(Monitor monitor) {
        if (monitor == null) {
            return null;
        }
        String identifier = monitor.getIdMonitor();
        if (identifier != null && !identifier.isBlank()) {
            return identifier;
        }
        return monitor.getCode();
    }

    private String buildMonitoringName(String courseName, String semester) {
        if (courseName == null && semester == null) {
            return null;
        }
        if (courseName == null) {
            return semester;
        }
        if (semester == null || semester.isBlank()) {
            return courseName;
        }
        return courseName + " - " + semester;
    }

    private String buildMonitorFullName(Monitor monitor) {
        if (monitor == null) {
            return null;
        }
        String first = Optional.ofNullable(monitor.getName()).orElse("");
        String last = Optional.ofNullable(monitor.getLastName()).orElse("");
        return (first + " " + last).trim();
    }

    private String resolveCurrentPeriod() {
        LocalDate now = LocalDate.now();
        int period = now.getMonthValue() <= 6 ? 1 : 2;
        return String.format(Locale.ROOT, "%04d-%d", now.getYear(), period);
    }

    private String normalizePeriod(String rawPeriod) {
        if (rawPeriod == null) {
            return null;
        }
        String normalized = rawPeriod.trim();
        return normalized.matches(PERIOD_REGEX) ? normalized : null;
    }

    private static class ResolvedAnswer {
        private final ProfessorSurveyQuestion question;
        private final int displayOrder;
        private final int score;

        private ResolvedAnswer(ProfessorSurveyQuestion question, int displayOrder, int score) {
            this.question = question;
            this.displayOrder = displayOrder;
            this.score = score;
        }

        private ProfessorSurveyQuestion getQuestion() {
            return question;
        }

        private int getDisplayOrder() {
            return displayOrder;
        }

        private int getScore() {
            return score;
        }
    }

    private static class EvaluationAnswersContext {
        private final List<ResolvedAnswer> orderedAnswers;
        private final double averageScore;
        private final int[] legacyScores;

        private EvaluationAnswersContext(List<ResolvedAnswer> orderedAnswers, double averageScore, int[] legacyScores) {
            this.orderedAnswers = orderedAnswers;
            this.averageScore = averageScore;
            this.legacyScores = legacyScores;
        }

        private List<ResolvedAnswer> getOrderedAnswers() {
            return orderedAnswers;
        }

        private double getAverageScore() {
            return averageScore;
        }

        private int[] getLegacyScores() {
            return legacyScores;
        }
    }
}
