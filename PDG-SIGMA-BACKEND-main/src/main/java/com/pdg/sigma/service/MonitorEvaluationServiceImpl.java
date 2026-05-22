package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitorEvaluationAssignmentDTO;
import com.pdg.sigma.dto.MonitorEvaluationRequest;
import com.pdg.sigma.dto.MonitorEvaluationResponse;
import com.pdg.sigma.repository.MonitorEvaluationRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MonitorEvaluationServiceImpl implements MonitorEvaluationService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;
    private static final int EDIT_WINDOW_YEARS = 1;

    @Autowired
    private MonitorEvaluationRepository monitorEvaluationRepository;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Override
    @Transactional
    public MonitorEvaluationResponse createEvaluation(String professorId, MonitorEvaluationRequest request) throws Exception {
        String ownerId = resolveProfessorId(professorId, request);
        validateRequest(ownerId, request);

        Monitoring monitoring = monitoringRepository.findById(request.getMonitoringId())
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));

        ensureOwnership(monitoring, ownerId);

        Monitor monitor = resolveMonitorForEvaluation(request.getMonitorCode(), monitoring);
        MonitoringMonitor monitoringMonitor = monitoringMonitorRepository
                .findByMonitoringIdAndMonitorCode(monitoring.getId(), monitor.getCode())
                .orElse(null);

        if (monitoringMonitor != null && !isEligibleStatus(monitoringMonitor.getEstadoSeleccion())) {
            throw new Exception("El monitor todavía no ha sido confirmado en la monitoría");
        }

        monitorEvaluationRepository.findByMonitoringIdAndMonitorCode(monitoring.getId(), monitor.getCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Ya existe una evaluación registrada para este monitor en la monitoría seleccionada");
                });

    Professor professor = professorRepository.findById(ownerId)
                .orElseThrow(() -> new Exception("Profesor no encontrado"));

        MonitorEvaluation evaluation = new MonitorEvaluation();
        evaluation.setMonitoring(monitoring);
        evaluation.setMonitor(monitor);
        evaluation.setProfessor(professor);
        evaluation.setMonitoringMonitor(monitoringMonitor);
        evaluation.setSemester(Optional.ofNullable(monitoring.getSemester()).orElse(null));

        boolean visible = request.getVisibleToMonitor() == null || Boolean.TRUE.equals(request.getVisibleToMonitor());
        evaluation.setVisibleToMonitor(visible);
        evaluation.setAcknowledgedByMonitor(false);
        evaluation.setAcknowledgedAt(null);
        evaluation.applyScores(
                request.getTaskCompliance(),
                request.getTimelyCommunication(),
                request.getPlanFulfillment(),
                request.getAttitude(),
                request.getComments()
        );

        MonitorEvaluation saved = monitorEvaluationRepository.save(evaluation);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MonitorEvaluationResponse updateEvaluation(Long evaluationId, String professorId, MonitorEvaluationRequest request) throws Exception {
        validateScores(request);

        MonitorEvaluation evaluation = monitorEvaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new Exception("Evaluación no encontrada"));

        if (evaluation.getProfessor() == null || !evaluation.getProfessor().getId().equals(professorId)) {
            throw new Exception("No está autorizado para editar esta evaluación");
        }

        ensureEditable(evaluation);

        boolean visible = request.getVisibleToMonitor() == null ? evaluation.isVisibleToMonitor() : request.getVisibleToMonitor();
        evaluation.setVisibleToMonitor(visible);

        evaluation.applyScores(
                request.getTaskCompliance(),
                request.getTimelyCommunication(),
                request.getPlanFulfillment(),
                request.getAttitude(),
                request.getComments()
        );

        // Reiniciar la visibilidad para que el monitor vuelva a revisar la evaluación actualizada
        evaluation.setAcknowledgedByMonitor(false);
        evaluation.setAcknowledgedAt(null);

        MonitorEvaluation saved = monitorEvaluationRepository.save(evaluation);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorEvaluationResponse> getEvaluationsByProfessor(String professorId) throws Exception {
        if (!professorRepository.existsById(professorId)) {
            throw new Exception("Profesor no encontrado");
        }
        return monitorEvaluationRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorEvaluationAssignmentDTO> getEvaluationAssignmentsForProfessor(String professorId, Optional<String> search) throws Exception {
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new Exception("Profesor no encontrado"));

        List<Monitoring> monitorings = monitoringRepository.findByProfessor(professor);
        if (monitorings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> monitoringIds = monitorings.stream()
                .map(Monitoring::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, MonitorEvaluation> evaluationMap = monitoringIds.isEmpty()
                ? Collections.emptyMap()
                : monitorEvaluationRepository.findByMonitoringIds(monitoringIds).stream()
                .collect(Collectors.toMap(
                        eval -> buildKey(eval.getMonitoring().getId(), eval.getMonitor().getCode()),
                        Function.identity()
                ));

        String normalizedSearch = search.map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .orElse(null);

        List<MonitorEvaluationAssignmentDTO> result = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>();

        for (Monitoring monitoring : monitorings) {
            List<MonitoringMonitor> relations = monitoringMonitorRepository.findByMonitoring(monitoring);

            if (relations != null) {
                for (MonitoringMonitor relation : relations) {
                    if (relation == null || relation.getMonitor() == null || relation.getMonitoring() == null) {
                        continue;
                    }
                    if (!isEligibleStatus(relation.getEstadoSeleccion())) {
                        continue;
                    }
                    String key = buildKey(monitoring.getId(), relation.getMonitor().getCode());
                    if (!processedKeys.add(key)) {
                        continue;
                    }
                    MonitorEvaluation existing = evaluationMap.get(key);
                    MonitorEvaluationAssignmentDTO dto = buildAssignmentDTO(monitoring, relation.getMonitor(), relation, existing);
                    if (matchesSearch(dto, normalizedSearch)) {
                        result.add(dto);
                    }
                }
            }

            Monitor assignedMonitor = monitoring.getAssignedMonitor();
            if (assignedMonitor != null) {
                String key = buildKey(monitoring.getId(), assignedMonitor.getCode());
                if (processedKeys.add(key)) {
                    MonitorEvaluation existing = evaluationMap.get(key);
                    MonitorEvaluationAssignmentDTO dto = buildAssignmentDTO(monitoring, assignedMonitor, null, existing);
                    if (matchesSearch(dto, normalizedSearch)) {
                        result.add(dto);
                    }
                }
            }
        }

        result.sort(Comparator
                .comparing(MonitorEvaluationAssignmentDTO::isEvaluated)
                .thenComparing(MonitorEvaluationAssignmentDTO::getMonitoringName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(MonitorEvaluationAssignmentDTO::getMonitorFullName, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorEvaluationResponse> getEvaluationsForMonitor(String monitorIdentifier) throws Exception {
        Monitor monitor = resolveMonitor(monitorIdentifier);
        return monitorEvaluationRepository.findByMonitorCodeOrderByCreatedAtDesc(monitor.getCode()).stream()
                .filter(MonitorEvaluation::isVisibleToMonitor)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MonitorEvaluationResponse acknowledgeEvaluation(Long evaluationId, String monitorIdentifier) throws Exception {
        Monitor monitor = resolveMonitor(monitorIdentifier);
        MonitorEvaluation evaluation = monitorEvaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new Exception("Evaluación no encontrada"));

        if (!evaluation.isVisibleToMonitor()) {
            throw new Exception("La evaluación no está visible para el monitor");
        }
        if (evaluation.getMonitor() == null || !evaluation.getMonitor().getCode().equals(monitor.getCode())) {
            throw new Exception("Esta evaluación no pertenece al monitor indicado");
        }
        if (!evaluation.isAcknowledgedByMonitor()) {
            evaluation.markAcknowledged();
            monitorEvaluationRepository.save(evaluation);
        }
        return toResponse(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MonitorEvaluationResponse> getEvaluation(Long evaluationId) {
        return monitorEvaluationRepository.findById(evaluationId)
                .map(this::toResponse);
    }

    private void validateRequest(String professorId, MonitorEvaluationRequest request) throws Exception {
        if (professorId == null || professorId.trim().isEmpty()) {
            throw new Exception("El identificador del profesor es obligatorio");
        }
        if (request.getMonitoringId() == null) {
            throw new Exception("Debe especificar la monitoría a evaluar");
        }
        if (request.getMonitorCode() == null || request.getMonitorCode().trim().isEmpty()) {
            throw new Exception("Debe indicar el monitor a evaluar");
        }
        validateScores(request);
    }

    private String resolveProfessorId(String explicit, MonitorEvaluationRequest request) throws Exception {
        if (explicit != null && !explicit.trim().isEmpty()) {
            return explicit.trim();
        }
        if (request.getProfessorId() != null && !request.getProfessorId().trim().isEmpty()) {
            return request.getProfessorId().trim();
        }
        throw new Exception("El identificador del profesor es obligatorio");
    }

    private void validateScores(MonitorEvaluationRequest request) throws Exception {
        ensureScoreInRange("Cumplimiento de tareas", request.getTaskCompliance());
        ensureScoreInRange("Comunicación oportuna", request.getTimelyCommunication());
        ensureScoreInRange("Cumplimiento del plan", request.getPlanFulfillment());
        ensureScoreInRange("Actitud", request.getAttitude());
    }

    private void ensureScoreInRange(String label, Integer value) throws Exception {
        if (value == null) {
            throw new Exception("Debe proporcionar una calificación para " + label);
        }
        if (value < MIN_SCORE || value > MAX_SCORE) {
            throw new Exception(label + " debe estar entre " + MIN_SCORE + " y " + MAX_SCORE);
        }
    }

    private void ensureOwnership(Monitoring monitoring, String professorId) throws Exception {
        if (monitoring.getProfessor() == null || !professorId.equals(monitoring.getProfessor().getId())) {
            throw new Exception("La monitoría no pertenece al profesor indicado");
        }
    }

    private void ensureEditable(MonitorEvaluation evaluation) throws Exception {
        LocalDateTime createdAt = evaluation.getCreatedAt();
        if (createdAt == null) {
            return;
        }
        LocalDateTime limit = LocalDateTime.now().minusYears(EDIT_WINDOW_YEARS);
        if (createdAt.isBefore(limit)) {
            throw new Exception("No se permite editar evaluaciones con antigüedad mayor a 1 año");
        }
    }

    private Monitor resolveMonitorForEvaluation(String monitorCode, Monitoring monitoring) throws Exception {
        if (monitorCode == null) {
            throw new Exception("Debe indicar el monitor a evaluar");
        }

        String trimmed = monitorCode.trim();

        // Primero intentar con el código directo (clave primaria)
        Optional<Monitor> monitorByCode = monitorRepository.findById(trimmed);
        if (monitorByCode.isPresent()) {
            return monitorByCode.get();
        }

        Optional<Monitor> monitorByExplicitCode = monitorRepository.findByCode(trimmed);
        if (monitorByExplicitCode.isPresent()) {
            return monitorByExplicitCode.get();
        }

        // Como fallback intentar por identificador alterno
        Optional<Monitor> monitorByDocument = monitorRepository.findByIdMonitor(trimmed);
        if (monitorByDocument.isPresent()) {
            return monitorByDocument.get();
        }

        // Si la monitoría tiene monitor asignado directamente, validar contra él
        Monitor assigned = monitoring.getAssignedMonitor();
        if (assigned != null && (trimmed.equalsIgnoreCase(assigned.getCode()) || trimmed.equalsIgnoreCase(assigned.getIdMonitor()))) {
            return assigned;
        }

        throw new Exception("Monitor no encontrado para la evaluación");
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

    private boolean isEligibleStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return "seleccionado".equals(normalized) || "aprobado".equals(normalized) || "asignado".equals(normalized);
    }

    private String buildKey(Long monitoringId, String monitorCode) {
        return monitoringId + "::" + monitorCode;
    }

    private MonitorEvaluationAssignmentDTO buildAssignmentDTO(Monitoring monitoring, Monitor monitor, MonitoringMonitor relation, MonitorEvaluation evaluation) {
        MonitorEvaluationAssignmentDTO dto = new MonitorEvaluationAssignmentDTO();
        dto.setMonitoringId(monitoring.getId());
        dto.setMonitoringMonitorId(relation != null ? relation.getId() : null);
        dto.setEvaluationId(evaluation != null ? evaluation.getId() : null);

        String courseName = monitoring.getCourse() != null ? monitoring.getCourse().getName() : null;
        String programName = monitoring.getProgram() != null ? monitoring.getProgram().getName() : null;
        String semester = resolveEvaluationSemester(evaluation, monitoring);
        String monitoringName = buildMonitoringName(courseName, semester);

        dto.setMonitoringName(monitoringName);
        dto.setCourseName(courseName);
        dto.setProgramName(programName);
        dto.setSemester(semester);

        dto.setMonitorCode(monitor != null ? monitor.getCode() : null);
        dto.setMonitorIdentifier(monitor != null ? monitor.getIdMonitor() : null);
        dto.setMonitorFullName(buildMonitorFullName(monitor));
        dto.setMonitorEmail(monitor != null ? monitor.getEmail() : null);

        if (evaluation != null) {
            dto.setEvaluated(true);
            dto.setTotalScore(evaluation.getTotalScore());
            dto.setPerformanceLevel(evaluation.getPerformanceLevel());
            dto.setPenaltyFlag(evaluation.isPenaltyFlag());
            dto.setVisibleToMonitor(evaluation.isVisibleToMonitor());
            dto.setAcknowledgedByMonitor(evaluation.isAcknowledgedByMonitor());
            dto.setEvaluatedAt(evaluation.getCreatedAt());
            dto.setTaskCompliance(evaluation.getTaskCompliance());
            dto.setTimelyCommunication(evaluation.getTimelyCommunication());
            dto.setPlanFulfillment(evaluation.getPlanFulfillment());
            dto.setAttitude(evaluation.getAttitude());
            dto.setComments(evaluation.getQualitativeFeedback());
        } else {
            dto.setEvaluated(false);
            dto.setVisibleToMonitor(true);
            dto.setPenaltyFlag(false);
        }

        return dto;
    }

    private MonitorEvaluationResponse toResponse(MonitorEvaluation evaluation) {
        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        response.setEvaluationId(evaluation.getId());
        response.setMonitoringId(evaluation.getMonitoring() != null ? evaluation.getMonitoring().getId() : null);
        response.setMonitoringMonitorId(evaluation.getMonitoringMonitor() != null ? evaluation.getMonitoringMonitor().getId() : null);

        Monitoring monitoring = evaluation.getMonitoring();
        if (monitoring != null) {
            String courseName = monitoring.getCourse() != null ? monitoring.getCourse().getName() : null;
            String programName = monitoring.getProgram() != null ? monitoring.getProgram().getName() : null;
            String semester = resolveEvaluationSemester(evaluation, monitoring);
            response.setMonitoringName(buildMonitoringName(courseName, semester));
            response.setCourseName(courseName);
            response.setProgramName(programName);
            response.setSemester(semester);
            response.setProfessorId(monitoring.getProfessor() != null ? monitoring.getProfessor().getId() : null);
        } else {
            response.setMonitoringName(null);
        }

        Monitor monitor = evaluation.getMonitor();
        if (monitor != null) {
            response.setMonitorCode(monitor.getCode());
            response.setMonitorIdentifier(monitor.getIdMonitor());
            response.setMonitorFullName(buildMonitorFullName(monitor));
            response.setMonitorEmail(monitor.getEmail());
        }

        response.setTaskCompliance(evaluation.getTaskCompliance());
        response.setTimelyCommunication(evaluation.getTimelyCommunication());
        response.setPlanFulfillment(evaluation.getPlanFulfillment());
        response.setAttitude(evaluation.getAttitude());
        response.setTotalScore(evaluation.getTotalScore());
        response.setPerformanceLevel(evaluation.getPerformanceLevel());
        response.setPenaltyFlag(evaluation.isPenaltyFlag());
        response.setPenaltyWeight(evaluation.getPenaltyWeight());
        response.setComments(evaluation.getQualitativeFeedback());
        response.setVisibleToMonitor(evaluation.isVisibleToMonitor());
        response.setAcknowledgedByMonitor(evaluation.isAcknowledgedByMonitor());
        response.setAcknowledgedAt(evaluation.getAcknowledgedAt());
        response.setCreatedAt(evaluation.getCreatedAt());
        response.setUpdatedAt(evaluation.getUpdatedAt());
        return response;
    }

    private boolean matchesSearch(MonitorEvaluationAssignmentDTO dto, String search) {
        if (search == null) {
            return true;
        }
        return contains(dto.getMonitorFullName(), search)
                || contains(dto.getMonitorCode(), search)
                || contains(dto.getMonitorIdentifier(), search)
                || contains(dto.getCourseName(), search)
                || contains(dto.getProgramName(), search)
                || contains(dto.getMonitoringName(), search)
                || (dto.getMonitoringId() != null && dto.getMonitoringId().toString().contains(search));
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
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

    private String resolveEvaluationSemester(MonitorEvaluation evaluation, Monitoring monitoring) {
        if (evaluation != null) {
            String evaluationSemester = evaluation.getSemester();
            if (evaluationSemester != null && !evaluationSemester.isBlank()) {
                return evaluationSemester;
            }
        }
        return monitoring != null ? monitoring.getSemester() : null;
    }
}
