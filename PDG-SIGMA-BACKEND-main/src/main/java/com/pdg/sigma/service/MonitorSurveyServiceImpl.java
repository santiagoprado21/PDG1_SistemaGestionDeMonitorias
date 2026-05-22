package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MonitorSurveyServiceImpl implements MonitorSurveyService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 7;

    @Autowired
    private MonitorSurveyQuestionRepository questionRepository;

    @Autowired
    private MonitorSurveySemesterConfigRepository semesterConfigRepository;

    @Autowired
    private MonitorSurveySemesterQuestionRepository semesterQuestionRepository;

    @Autowired
    private MonitorSurveyTemplateRepository templateRepository;

    @Autowired
    private MonitorSurveyTemplateQuestionRepository templateQuestionRepository;

    @Autowired
    private MonitorSurveyResponseRepository responseRepository;

    @Autowired
    private MonitorSurveyResponseAnswerRepository responseAnswerRepository;

    @Autowired
    private MonitorSurveyIntegrationConfigRepository integrationConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MonitorSurveyQuestionDTO> getQuestionBank(String semester) {
        MonitorSurveyCurrentConfigDTO currentConfig = getCurrentConfig(semester);
        Map<Long, Integer> orderMap = currentConfig.getQuestions().stream()
                .filter(q -> q.getId() != null && q.getDisplayOrder() != null)
                .collect(Collectors.toMap(MonitorSurveyQuestionDTO::getId, MonitorSurveyQuestionDTO::getDisplayOrder));

        return questionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(question -> toQuestionDTO(question, orderMap.get(question.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MonitorSurveyQuestionDTO createQuestion(MonitorSurveyQuestionCreateRequest request) throws Exception {
        String statement = normalizeRequired(request.getStatement(), "El texto de la pregunta es obligatorio");
        String category = normalizeRequired(request.getCategory(), "La categoría es obligatoria");

        MonitorSurveyQuestion question = new MonitorSurveyQuestion();
        question.setStatement(statement);
        question.setCategory(category);
        question.setBankActive(true);
        question.setQuestionKey(generateQuestionKey(category, statement));

        MonitorSurveyQuestion saved = questionRepository.save(question);
        return toQuestionDTO(saved, null);
    }

    @Override
    @Transactional
    public MonitorSurveyQuestionDTO updateQuestion(Long questionId, MonitorSurveyQuestionUpdateRequest request, String semester) throws Exception {
        MonitorSurveyQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new Exception("Pregunta no encontrada"));

        String targetSemester = resolveSemesterForEdition(semester);
        if (targetSemester != null && responseAnswerRepository.existsByQuestionIdAndResponseSemester(questionId, targetSemester)) {
            throw new Exception("No se puede editar la pregunta porque ya tiene respuestas asociadas en el semestre actual");
        }

        question.setStatement(normalizeRequired(request.getStatement(), "El texto de la pregunta es obligatorio"));
        question.setCategory(normalizeRequired(request.getCategory(), "La categoría es obligatoria"));

        MonitorSurveyQuestion saved = questionRepository.save(question);
        Integer displayOrder = getDisplayOrderInCurrentConfig(saved.getId(), semester);
        return toQuestionDTO(saved, displayOrder);
    }

    @Override
    @Transactional
    public MonitorSurveyQuestionDTO updateQuestionStatus(Long questionId, boolean bankActive) throws Exception {
        MonitorSurveyQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new Exception("Pregunta no encontrada"));

        question.setBankActive(bankActive);
        MonitorSurveyQuestion saved = questionRepository.save(question);

        if (!bankActive) {
            semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc().ifPresent(config -> {
                List<MonitorSurveySemesterQuestion> selected = semesterQuestionRepository
                        .findBySemesterConfigIdOrderByDisplayOrderAsc(config.getId());
                int order = 1;
                for (MonitorSurveySemesterQuestion entry : selected) {
                    if (entry.getQuestion() != null && Objects.equals(entry.getQuestion().getId(), questionId)) {
                        entry.setActive(false);
                    }
                    if (entry.isActive()) {
                        entry.setDisplayOrder(order++);
                    }
                }
                semesterQuestionRepository.saveAll(selected);
            });
        }

        Integer displayOrder = getDisplayOrderInCurrentConfig(saved.getId(), null);
        return toQuestionDTO(saved, displayOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorSurveyCurrentConfigDTO getCurrentConfig(String semester) {
        Optional<MonitorSurveySemesterConfig> configOpt = resolveConfig(semester);
        MonitorSurveyCurrentConfigDTO dto = new MonitorSurveyCurrentConfigDTO();

        if (configOpt.isEmpty()) {
            dto.setSemester(semester);
            dto.setQuestions(Collections.emptyList());
            return dto;
        }

        MonitorSurveySemesterConfig config = configOpt.get();
        List<MonitorSurveySemesterQuestion> selected = semesterQuestionRepository
                .findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(config.getId());

        List<MonitorSurveyQuestionDTO> questions = selected.stream()
                .filter(item -> item.getQuestion() != null)
                .map(item -> toQuestionDTO(item.getQuestion(), item.getDisplayOrder()))
                .collect(Collectors.toList());

        dto.setSemester(config.getSemester());
        dto.setQuestions(questions);
        return dto;
    }

    @Override
    @Transactional
    public MonitorSurveyCurrentConfigDTO saveCurrentConfig(MonitorSurveyCurrentConfigRequest request) throws Exception {
        String semester = normalizeRequiredPeriod(request.getSemester(), "El semestre es obligatorio");
        List<Long> questionIds = normalizeQuestionIds(request.getQuestionIds());

        if (questionIds.isEmpty()) {
            throw new Exception("Debe seleccionar al menos una pregunta para la encuesta activa");
        }

        List<MonitorSurveyQuestion> selectedQuestions = new ArrayList<>();
        for (Long id : questionIds) {
            MonitorSurveyQuestion question = questionRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pregunta no encontrada: " + id));
            if (!question.isBankActive()) {
                throw new Exception("La pregunta " + id + " está inactiva en el banco");
            }
            selectedQuestions.add(question);
        }

        // Dejar un único semestre activo para la encuesta de estudiantes
        semesterConfigRepository.findAllByOrderByUpdatedAtDesc().forEach(config -> {
            config.setActive(false);
            semesterConfigRepository.save(config);
        });

        MonitorSurveySemesterConfig config = semesterConfigRepository.findBySemester(semester)
                .orElseGet(MonitorSurveySemesterConfig::new);
        config.setSemester(semester);
        config.setActive(true);
        MonitorSurveySemesterConfig savedConfig = semesterConfigRepository.save(config);

        semesterQuestionRepository.deleteBySemesterConfigId(savedConfig.getId());
        semesterQuestionRepository.flush();

        int order = 1;
        for (MonitorSurveyQuestion question : selectedQuestions) {
            MonitorSurveySemesterQuestion entry = new MonitorSurveySemesterQuestion();
            entry.setSemesterConfig(savedConfig);
            entry.setQuestion(question);
            entry.setDisplayOrder(order++);
            entry.setActive(true);
            semesterQuestionRepository.save(entry);
        }

        return getCurrentConfig(semester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorSurveyTemplateDTO> listTemplates() {
        return templateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toTemplateDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MonitorSurveyTemplateDTO createTemplate(MonitorSurveyTemplateCreateRequest request) throws Exception {
        String name = normalizeRequired(request.getName(), "El nombre de la plantilla es obligatorio");
        String createdForSemester = normalizeRequiredPeriod(request.getCreatedForSemester(), "El periodo de creación de la plantilla es obligatorio");
        List<Long> questionIds = normalizeQuestionIds(request.getQuestionIds());

        if (questionIds.isEmpty()) {
            throw new Exception("Debe seleccionar preguntas para crear la plantilla");
        }

        MonitorSurveyTemplate template = new MonitorSurveyTemplate();
        template.setName(name);
        template.setDescription(request.getDescription());
        template.setCreatedForSemester(createdForSemester);
        MonitorSurveyTemplate savedTemplate = templateRepository.save(template);

        int order = 1;
        for (Long id : questionIds) {
            MonitorSurveyQuestion question = questionRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pregunta no encontrada: " + id));
            MonitorSurveyTemplateQuestion entry = new MonitorSurveyTemplateQuestion();
            entry.setTemplate(savedTemplate);
            entry.setQuestion(question);
            entry.setDisplayOrder(order++);
            templateQuestionRepository.save(entry);
        }

        return toTemplateDTO(savedTemplate);
    }

    @Override
    @Transactional
    public MonitorSurveyTemplateDTO updateTemplate(Long templateId, MonitorSurveyTemplateUpdateRequest request) throws Exception {
        MonitorSurveyTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new Exception("Plantilla no encontrada"));

        String name = normalizeRequired(request.getName(), "El nombre de la plantilla es obligatorio");
        String createdForSemester = normalizeRequiredPeriod(request.getCreatedForSemester(), "El periodo de creación de la plantilla es obligatorio");
        List<Long> questionIds = normalizeQuestionIds(request.getQuestionIds());

        if (questionIds.isEmpty()) {
            throw new Exception("Debe seleccionar al menos una pregunta para actualizar la plantilla");
        }

        template.setName(name);
        template.setDescription(request.getDescription());
        template.setCreatedForSemester(createdForSemester);
        MonitorSurveyTemplate savedTemplate = templateRepository.save(template);

        templateQuestionRepository.deleteByTemplateId(savedTemplate.getId());
        templateQuestionRepository.flush();

        int order = 1;
        for (Long id : questionIds) {
            MonitorSurveyQuestion question = questionRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pregunta no encontrada: " + id));
            MonitorSurveyTemplateQuestion entry = new MonitorSurveyTemplateQuestion();
            entry.setTemplate(savedTemplate);
            entry.setQuestion(question);
            entry.setDisplayOrder(order++);
            templateQuestionRepository.save(entry);
        }

        return toTemplateDTO(savedTemplate);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) throws Exception {
        MonitorSurveyTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new Exception("Plantilla no encontrada"));

        List<MonitorSurveySemesterConfig> configs = semesterConfigRepository.findAllByTemplateId(templateId);
        for (MonitorSurveySemesterConfig config : configs) {
            config.setTemplate(null);
            semesterConfigRepository.save(config);
        }

        templateQuestionRepository.deleteByTemplateId(templateId);
        templateRepository.delete(template);
    }

    @Override
    @Transactional
    public MonitorSurveyCurrentConfigDTO applyTemplate(MonitorSurveyApplyTemplateRequest request) throws Exception {
        String semester = normalizeRequiredPeriod(request.getSemester(), "El semestre es obligatorio");
        if (request.getTemplateId() == null) {
            throw new Exception("Debe indicar la plantilla");
        }

        MonitorSurveyTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new Exception("Plantilla no encontrada"));

        List<Long> questionIds = normalizeQuestionIds(
            templateQuestionRepository.findByTemplateIdOrderByDisplayOrderAsc(template.getId()).stream()
                .map(item -> item.getQuestion().getId())
            .collect(Collectors.toList())
        );

        if (questionIds.isEmpty()) {
            throw new Exception("La plantilla seleccionada no tiene preguntas");
        }

        MonitorSurveyCurrentConfigRequest configRequest = new MonitorSurveyCurrentConfigRequest();
        configRequest.setSemester(semester);
        configRequest.setQuestionIds(questionIds);
        MonitorSurveyCurrentConfigDTO config = saveCurrentConfig(configRequest);

        semesterConfigRepository.findBySemester(semester).ifPresent(saved -> {
            saved.setTemplate(template);
            semesterConfigRepository.save(saved);
        });

        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorSurveyPublicQuestionDTO> getPublicQuestions(String semester) {
        MonitorSurveyCurrentConfigDTO config = getCurrentConfig(semester);
        return config.getQuestions().stream()
                .map(this::toPublicQuestionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void storePublicResponse(MonitorSurveyPublicResponseRequest request) throws Exception {
        String semester = normalizeRequired(request.getSemester(), "El semestre es obligatorio");
        List<MonitorSurveyPublicResponseAnswerDTO> answers = Optional.ofNullable(request.getAnswers()).orElse(Collections.emptyList());
        if (answers.isEmpty()) {
            throw new Exception("Debe incluir respuestas para la encuesta");
        }

        MonitorSurveyResponse response = new MonitorSurveyResponse();
        response.setSemester(semester);
        response.setMonitoringId(trimToNull(request.getMonitoringId()));
        response.setMonitorCode(trimToNull(request.getMonitorCode()));
        response.setMonitorName(trimToNull(request.getMonitorName()));
        response.setPositiveFeedback(trimToNull(request.getPositiveFeedback()));
        response.setImprovementFeedback(trimToNull(request.getImprovementFeedback()));
        response.setAverageScore(request.getAverageScore());

        MonitorSurveyResponse savedResponse = responseRepository.save(response);

        for (MonitorSurveyPublicResponseAnswerDTO answer : answers) {
            if (answer.getQuestionId() == null || answer.getScore() == null) {
                throw new Exception("Cada respuesta debe incluir pregunta y puntaje");
            }
            if (answer.getScore() < MIN_SCORE || answer.getScore() > MAX_SCORE) {
                throw new Exception("Los puntajes deben estar entre " + MIN_SCORE + " y " + MAX_SCORE);
            }
            MonitorSurveyQuestion question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new Exception("Pregunta no encontrada: " + answer.getQuestionId()));

            MonitorSurveyResponseAnswer answerRow = new MonitorSurveyResponseAnswer();
            answerRow.setResponse(savedResponse);
            answerRow.setQuestion(question);
            answerRow.setScore(answer.getScore());
            responseAnswerRepository.save(answerRow);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorSurveyIntegrationConfigDTO getIntegrationConfig() {
        MonitorSurveyIntegrationConfig config = integrationConfigRepository
                .findFirstByOrderByUpdatedAtDesc()
                .orElse(null);
        return toIntegrationConfigDTO(config);
    }

    @Override
    @Transactional
    public MonitorSurveyIntegrationConfigDTO saveIntegrationConfig(MonitorSurveyIntegrationConfigRequest request) throws Exception {
        MonitorSurveyIntegrationConfig config = integrationConfigRepository
                .findFirstByOrderByUpdatedAtDesc()
                .orElseGet(MonitorSurveyIntegrationConfig::new);

        config.setAppsScriptUrl(trimToNull(request.getAppsScriptUrl()));
        config.setDashboardUrl(trimToNull(request.getDashboardUrl()));

        MonitorSurveyIntegrationConfig saved = integrationConfigRepository.save(config);
        return toIntegrationConfigDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorSurveyReportDTO getSurveyReport(String semester, String monitorCode, String monitoringId) {
        ReportData reportData = loadReportData(semester, monitorCode, monitoringId);
        MonitorSurveyReportDTO dto = new MonitorSurveyReportDTO();
        dto.setSemester(reportData.semester);

        List<MonitorSurveyResponse> responses = reportData.responses;
        dto.setTotalResponses(responses.size());
        dto.setAverageScore(responses.stream()
                .map(MonitorSurveyResponse::getAverageScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));

        List<MonitorSurveyResponseSummaryDTO> responseSummaries = responses.stream()
                .map(this::toResponseSummary)
                .collect(Collectors.toList());
        dto.setResponses(responseSummaries);

        List<MonitorSurveyResponseAnswer> answers = reportData.answers;
        dto.setTotalAnswers(answers.size());

        Map<Long, List<MonitorSurveyResponseAnswer>> byQuestion = answers.stream()
                .filter(answer -> answer.getQuestion() != null)
                .collect(Collectors.groupingBy(answer -> answer.getQuestion().getId()));

        List<MonitorSurveyQuestionStatsDTO> stats = new ArrayList<>();
        for (Map.Entry<Long, List<MonitorSurveyResponseAnswer>> entry : byQuestion.entrySet()) {
            List<MonitorSurveyResponseAnswer> questionAnswers = entry.getValue();
            if (questionAnswers.isEmpty() || questionAnswers.get(0).getQuestion() == null) {
                continue;
            }
            MonitorSurveyQuestion question = questionAnswers.get(0).getQuestion();
            DoubleSummaryStatistics summary = questionAnswers.stream()
                    .mapToDouble(MonitorSurveyResponseAnswer::getScore)
                    .summaryStatistics();

            MonitorSurveyQuestionStatsDTO stat = new MonitorSurveyQuestionStatsDTO();
            stat.setQuestionId(question.getId());
            stat.setQuestionKey(question.getQuestionKey());
            stat.setStatement(question.getStatement());
            stat.setCategory(question.getCategory());
            stat.setResponsesCount((int) summary.getCount());
            stat.setAverageScore(summary.getAverage());
            stat.setMinScore((int) summary.getMin());
            stat.setMaxScore((int) summary.getMax());
            stats.add(stat);
        }

        stats.sort(Comparator
                .comparing(MonitorSurveyQuestionStatsDTO::getCategory, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(MonitorSurveyQuestionStatsDTO::getStatement, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        dto.setQuestionStats(stats);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportSurveyReportCsv(String semester, String monitorCode, String monitoringId) {
        ReportData reportData = loadReportData(semester, monitorCode, monitoringId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("response_id,created_at,semester,monitoring_id,monitor_code,monitor_name,average_score,question_id,question_key,question_category,question_statement,score,positive_feedback,improvement_feedback\n");

        for (MonitorSurveyResponseAnswer answer : reportData.answers) {
            MonitorSurveyResponse response = answer.getResponse();
            MonitorSurveyQuestion question = answer.getQuestion();

            csv.append(escapeCsv(asString(response != null ? response.getId() : null))).append(',');
            csv.append(escapeCsv(response != null && response.getCreatedAt() != null ? formatter.format(response.getCreatedAt()) : null)).append(',');
            csv.append(escapeCsv(response != null ? response.getSemester() : null)).append(',');
            csv.append(escapeCsv(response != null ? response.getMonitoringId() : null)).append(',');
            csv.append(escapeCsv(response != null ? response.getMonitorCode() : null)).append(',');
            csv.append(escapeCsv(response != null ? response.getMonitorName() : null)).append(',');
            csv.append(escapeCsv(response != null ? asString(response.getAverageScore()) : null)).append(',');
            csv.append(escapeCsv(question != null ? asString(question.getId()) : null)).append(',');
            csv.append(escapeCsv(question != null ? question.getQuestionKey() : null)).append(',');
            csv.append(escapeCsv(question != null ? question.getCategory() : null)).append(',');
            csv.append(escapeCsv(question != null ? question.getStatement() : null)).append(',');
            csv.append(escapeCsv(asString(answer.getScore()))).append(',');
            csv.append(escapeCsv(response != null ? response.getPositiveFeedback() : null)).append(',');
            csv.append(escapeCsv(response != null ? response.getImprovementFeedback() : null)).append('\n');
        }

        return csv.toString();
    }

    private Optional<MonitorSurveySemesterConfig> resolveConfig(String semester) {
        if (semester != null && !semester.isBlank()) {
            return semesterConfigRepository.findBySemester(semester.trim());
        }
        return semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc();
    }

    private Integer getDisplayOrderInCurrentConfig(Long questionId, String semester) {
        Optional<MonitorSurveySemesterConfig> configOpt = resolveConfig(semester);
        if (configOpt.isEmpty()) {
            return null;
        }
        return semesterQuestionRepository.findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(configOpt.get().getId()).stream()
                .filter(item -> item.getQuestion() != null && Objects.equals(item.getQuestion().getId(), questionId))
                .map(MonitorSurveySemesterQuestion::getDisplayOrder)
                .findFirst()
                .orElse(null);
    }

    private String resolveSemesterForEdition(String semester) {
        if (semester != null && !semester.isBlank()) {
            return semester.trim();
        }
        return semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .map(MonitorSurveySemesterConfig::getSemester)
                .orElse(null);
    }

    private List<Long> normalizeQuestionIds(List<Long> rawQuestionIds) {
        return Optional.ofNullable(rawQuestionIds).orElse(Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private MonitorSurveyQuestionDTO toQuestionDTO(MonitorSurveyQuestion question, Integer displayOrder) {
        MonitorSurveyQuestionDTO dto = new MonitorSurveyQuestionDTO();
        dto.setId(question.getId());
        dto.setQuestionKey(question.getQuestionKey());
        dto.setStatement(question.getStatement());
        dto.setCategory(question.getCategory());
        dto.setBankActive(question.isBankActive());
        dto.setDisplayOrder(displayOrder);
        dto.setSelectedInCurrentSurvey(displayOrder != null);
        return dto;
    }

    private MonitorSurveyTemplateDTO toTemplateDTO(MonitorSurveyTemplate template) {
        MonitorSurveyTemplateDTO dto = new MonitorSurveyTemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setCreatedForSemester(template.getCreatedForSemester());

        List<MonitorSurveyQuestionDTO> questions = templateQuestionRepository.findByTemplateIdOrderByDisplayOrderAsc(template.getId()).stream()
                .filter(item -> item.getQuestion() != null)
                .map(item -> toQuestionDTO(item.getQuestion(), item.getDisplayOrder()))
                .collect(Collectors.toList());
        dto.setQuestions(questions);
        return dto;
    }

    private MonitorSurveyPublicQuestionDTO toPublicQuestionDTO(MonitorSurveyQuestionDTO question) {
        MonitorSurveyPublicQuestionDTO dto = new MonitorSurveyPublicQuestionDTO();
        dto.setId(question.getId());
        dto.setQuestionKey(question.getQuestionKey());
        dto.setStatement(question.getStatement());
        dto.setCategory(question.getCategory());
        dto.setDisplayOrder(question.getDisplayOrder() == null ? 0 : question.getDisplayOrder());
        return dto;
    }

    private MonitorSurveyIntegrationConfigDTO toIntegrationConfigDTO(MonitorSurveyIntegrationConfig config) {
        MonitorSurveyIntegrationConfigDTO dto = new MonitorSurveyIntegrationConfigDTO();
        if (config == null) {
            dto.setAppsScriptUrl(null);
            dto.setDashboardUrl(null);
            return dto;
        }
        dto.setAppsScriptUrl(config.getAppsScriptUrl());
        dto.setDashboardUrl(config.getDashboardUrl());
        return dto;
    }

    private MonitorSurveyResponseSummaryDTO toResponseSummary(MonitorSurveyResponse response) {
        MonitorSurveyResponseSummaryDTO dto = new MonitorSurveyResponseSummaryDTO();
        dto.setResponseId(response.getId());
        dto.setSemester(response.getSemester());
        dto.setMonitoringId(response.getMonitoringId());
        dto.setMonitorCode(response.getMonitorCode());
        dto.setMonitorName(response.getMonitorName());
        dto.setAverageScore(response.getAverageScore());
        dto.setPositiveFeedback(response.getPositiveFeedback());
        dto.setImprovementFeedback(response.getImprovementFeedback());
        dto.setCreatedAt(response.getCreatedAt());
        return dto;
    }

    private ReportData loadReportData(String semester, String monitorCode, String monitoringId) {
        String normalizedSemester = trimToNull(semester);
        String normalizedMonitorCode = trimToNull(monitorCode);
        String normalizedMonitoringId = trimToNull(monitoringId);

        List<MonitorSurveyResponse> responses = responseRepository
                .findByFilters(normalizedSemester, normalizedMonitorCode, normalizedMonitoringId);
        if (responses.isEmpty()) {
            return new ReportData(normalizedSemester, Collections.emptyList(), Collections.emptyList());
        }

        List<Long> responseIds = responses.stream()
                .map(MonitorSurveyResponse::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (responseIds.isEmpty()) {
            return new ReportData(normalizedSemester, responses, Collections.emptyList());
        }

        List<MonitorSurveyResponseAnswer> answers = responseAnswerRepository.findByResponseIdInWithDetails(responseIds);
        return new ReportData(normalizedSemester, responses, answers);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeRequired(String value, String errorMessage) throws Exception {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new Exception(errorMessage);
        }
        return normalized;
    }

    private String normalizeRequiredPeriod(String value, String errorMessage) throws Exception {
        String normalized = normalizeRequired(value, errorMessage);
        if (!normalized.matches("^\\d{4}-[12]$")) {
            throw new Exception("El periodo debe tener formato AAAA-1 o AAAA-2");
        }
        if (Integer.parseInt(normalized.substring(0, 4)) != LocalDate.now().getYear()) {
            throw new Exception("El año del periodo debe corresponder al año actual");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateQuestionKey(String category, String statement) {
        String categoryPart = Optional.ofNullable(category).orElse("general");
        String statementPart = Optional.ofNullable(statement).orElse("pregunta");

        String base = (normalizeForKey(categoryPart) + "_" + normalizeForKey(statementPart))
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (base.length() > 50) {
            base = base.substring(0, 50);
        }

        String candidate = base + "_" + System.currentTimeMillis();
        if (candidate.length() > 80) {
            candidate = candidate.substring(0, 80);
        }

        while (questionRepository.findByQuestionKey(candidate).isPresent()) {
            candidate = base + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            if (candidate.length() > 80) {
                candidate = candidate.substring(0, 80);
            }
        }
        return candidate;
    }

    private String normalizeForKey(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_");

        return normalized.isBlank() ? "item" : normalized;
    }

    private static class ReportData {
        private final String semester;
        private final List<MonitorSurveyResponse> responses;
        private final List<MonitorSurveyResponseAnswer> answers;

        private ReportData(String semester, List<MonitorSurveyResponse> responses, List<MonitorSurveyResponseAnswer> answers) {
            this.semester = semester;
            this.responses = responses;
            this.answers = answers;
        }
    }
}
