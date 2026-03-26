package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
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
        String semester = normalizeRequired(request.getSemester(), "El semestre es obligatorio");
        List<Long> questionIds = Optional.ofNullable(request.getQuestionIds()).orElse(Collections.emptyList());

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
        List<Long> questionIds = Optional.ofNullable(request.getQuestionIds()).orElse(Collections.emptyList());

        if (questionIds.isEmpty()) {
            throw new Exception("Debe seleccionar preguntas para crear la plantilla");
        }

        MonitorSurveyTemplate template = new MonitorSurveyTemplate();
        template.setName(name);
        template.setDescription(request.getDescription());
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
    public MonitorSurveyCurrentConfigDTO applyTemplate(MonitorSurveyApplyTemplateRequest request) throws Exception {
        String semester = normalizeRequired(request.getSemester(), "El semestre es obligatorio");
        if (request.getTemplateId() == null) {
            throw new Exception("Debe indicar la plantilla");
        }

        MonitorSurveyTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new Exception("Plantilla no encontrada"));

        List<Long> questionIds = templateQuestionRepository.findByTemplateIdOrderByDisplayOrderAsc(template.getId()).stream()
                .map(item -> item.getQuestion().getId())
                .collect(Collectors.toList());

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

    private String normalizeRequired(String value, String errorMessage) throws Exception {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new Exception(errorMessage);
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
}
