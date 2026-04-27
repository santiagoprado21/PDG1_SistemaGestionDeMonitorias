package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfessorSurveyServiceImpl implements ProfessorSurveyService {

    private static final String PERIOD_REGEX = "^\\d{4}-[12]$";

    @Autowired
    private ProfessorSurveyQuestionRepository questionRepository;

    @Autowired
    private ProfessorSurveySemesterConfigRepository semesterConfigRepository;

    @Autowired
    private ProfessorSurveySemesterQuestionRepository semesterQuestionRepository;

    @Autowired
    private ProfessorSurveyTemplateRepository templateRepository;

    @Autowired
    private ProfessorSurveyTemplateQuestionRepository templateQuestionRepository;

    @Autowired
    private SupervisorEvaluationAnswerRepository supervisorEvaluationAnswerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProfessorSurveyQuestionDTO> getQuestionBank(String semester) {
        ProfessorSurveyCurrentConfigDTO currentConfig = getCurrentConfig(semester);
        Map<Long, Integer> orderMap = currentConfig.getQuestions().stream()
                .filter(q -> q.getId() != null && q.getDisplayOrder() != null)
                .collect(Collectors.toMap(ProfessorSurveyQuestionDTO::getId, ProfessorSurveyQuestionDTO::getDisplayOrder));

        return questionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(question -> toQuestionDTO(question, orderMap.get(question.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProfessorSurveyQuestionDTO createQuestion(ProfessorSurveyQuestionCreateRequest request) throws Exception {
        String statement = normalizeRequired(request.getStatement(), "El texto de la pregunta es obligatorio");
        String category = normalizeRequired(request.getCategory(), "La categoría es obligatoria");

        ProfessorSurveyQuestion question = new ProfessorSurveyQuestion();
        question.setStatement(statement);
        question.setCategory(category);
        question.setBankActive(true);
        question.setQuestionKey(generateQuestionKey(category, statement));

        ProfessorSurveyQuestion saved = questionRepository.save(question);
        return toQuestionDTO(saved, null);
    }

    @Override
    @Transactional
    public ProfessorSurveyQuestionDTO updateQuestion(Long questionId, ProfessorSurveyQuestionUpdateRequest request, String semester) throws Exception {
        ProfessorSurveyQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new Exception("Pregunta no encontrada"));

        String targetSemester = resolveSemesterForEdition(semester);
        if (targetSemester != null
            && supervisorEvaluationAnswerRepository.existsByQuestionIdAndEvaluationSemester(questionId, targetSemester)) {
            throw new Exception("No se puede editar la pregunta porque ya tiene respuestas asociadas en el periodo actual");
        }

        question.setStatement(normalizeRequired(request.getStatement(), "El texto de la pregunta es obligatorio"));
        question.setCategory(normalizeRequired(request.getCategory(), "La categoría es obligatoria"));

        ProfessorSurveyQuestion saved = questionRepository.save(question);
        Integer displayOrder = getDisplayOrderInCurrentConfig(saved.getId(), semester);
        return toQuestionDTO(saved, displayOrder);
    }

    @Override
    @Transactional
    public ProfessorSurveyQuestionDTO updateQuestionStatus(Long questionId, boolean bankActive) throws Exception {
        ProfessorSurveyQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new Exception("Pregunta no encontrada"));

        question.setBankActive(bankActive);
        ProfessorSurveyQuestion saved = questionRepository.save(question);

        if (!bankActive) {
            semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc().ifPresent(config -> {
                List<ProfessorSurveySemesterQuestion> selected = semesterQuestionRepository
                        .findBySemesterConfigIdOrderByDisplayOrderAsc(config.getId());
                int order = 1;
                for (ProfessorSurveySemesterQuestion entry : selected) {
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
    public ProfessorSurveyCurrentConfigDTO getCurrentConfig(String semester) {
        Optional<ProfessorSurveySemesterConfig> configOpt = resolveConfig(semester);
        ProfessorSurveyCurrentConfigDTO dto = new ProfessorSurveyCurrentConfigDTO();

        if (configOpt.isEmpty()) {
            String requestedPeriod = normalizeOptionalPeriod(semester);
            dto.setSemester(requestedPeriod != null ? requestedPeriod : resolveCurrentPeriod());
            dto.setQuestions(Collections.emptyList());
            return dto;
        }

        ProfessorSurveySemesterConfig config = configOpt.get();
        List<ProfessorSurveySemesterQuestion> selected = semesterQuestionRepository
                .findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(config.getId());

        List<ProfessorSurveyQuestionDTO> questions = selected.stream()
                .filter(item -> item.getQuestion() != null)
                .map(item -> toQuestionDTO(item.getQuestion(), item.getDisplayOrder()))
                .collect(Collectors.toList());

        String resolvedPeriod = normalizeOptionalPeriod(config.getSemester());
        dto.setSemester(resolvedPeriod != null ? resolvedPeriod : resolveCurrentPeriod());
        dto.setQuestions(questions);
        return dto;
    }

    @Override
    @Transactional
    public ProfessorSurveyCurrentConfigDTO saveCurrentConfig(ProfessorSurveyCurrentConfigRequest request) throws Exception {
        String semester = normalizeRequiredPeriod(request.getSemester(), "El periodo es obligatorio");
        List<Long> questionIds = normalizeQuestionIds(request.getQuestionIds());

        if (questionIds.isEmpty()) {
            throw new Exception("Debe seleccionar al menos una pregunta para la encuesta activa");
        }

        List<ProfessorSurveyQuestion> selectedQuestions = new ArrayList<>();
        for (Long id : questionIds) {
            ProfessorSurveyQuestion question = questionRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pregunta no encontrada: " + id));
            if (!question.isBankActive()) {
                throw new Exception("La pregunta " + id + " está inactiva en el banco");
            }
            selectedQuestions.add(question);
        }

        // Dejar un único periodo activo para la encuesta de evaluación de profesores
        semesterConfigRepository.findAllByOrderByUpdatedAtDesc().forEach(config -> {
            config.setActive(false);
            semesterConfigRepository.save(config);
        });

        ProfessorSurveySemesterConfig config = semesterConfigRepository.findBySemester(semester)
                .orElseGet(ProfessorSurveySemesterConfig::new);
        config.setSemester(semester);
        config.setActive(true);
        ProfessorSurveySemesterConfig savedConfig = semesterConfigRepository.save(config);

        semesterQuestionRepository.deleteBySemesterConfigId(savedConfig.getId());
        semesterQuestionRepository.flush();

        int order = 1;
        for (ProfessorSurveyQuestion question : selectedQuestions) {
            ProfessorSurveySemesterQuestion entry = new ProfessorSurveySemesterQuestion();
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
    public List<ProfessorSurveyTemplateDTO> listTemplates() {
        return templateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toTemplateDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProfessorSurveyTemplateDTO createTemplate(ProfessorSurveyTemplateCreateRequest request) throws Exception {
        String name = normalizeRequired(request.getName(), "El nombre de la plantilla es obligatorio");
        String createdForSemester = normalizeRequiredPeriod(request.getCreatedForSemester(), "El periodo de creación de la plantilla es obligatorio");
        List<Long> questionIds = normalizeQuestionIds(request.getQuestionIds());

        if (questionIds.isEmpty()) {
            throw new Exception("Debe seleccionar preguntas para crear la plantilla");
        }

        ProfessorSurveyTemplate template = new ProfessorSurveyTemplate();
        template.setName(name);
        template.setDescription(request.getDescription());
        template.setCreatedForSemester(createdForSemester);
        ProfessorSurveyTemplate savedTemplate = templateRepository.save(template);

        int order = 1;
        for (Long id : questionIds) {
            ProfessorSurveyQuestion question = questionRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pregunta no encontrada: " + id));
            ProfessorSurveyTemplateQuestion entry = new ProfessorSurveyTemplateQuestion();
            entry.setTemplate(savedTemplate);
            entry.setQuestion(question);
            entry.setDisplayOrder(order++);
            templateQuestionRepository.save(entry);
        }

        return toTemplateDTO(savedTemplate);
    }

    @Override
    @Transactional
    public ProfessorSurveyTemplateDTO updateTemplate(Long templateId, ProfessorSurveyTemplateUpdateRequest request) throws Exception {
        ProfessorSurveyTemplate template = templateRepository.findById(templateId)
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
        ProfessorSurveyTemplate savedTemplate = templateRepository.save(template);

        templateQuestionRepository.deleteByTemplateId(savedTemplate.getId());
        templateQuestionRepository.flush();

        int order = 1;
        for (Long id : questionIds) {
            ProfessorSurveyQuestion question = questionRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pregunta no encontrada: " + id));
            ProfessorSurveyTemplateQuestion entry = new ProfessorSurveyTemplateQuestion();
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
        ProfessorSurveyTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new Exception("Plantilla no encontrada"));

        List<ProfessorSurveySemesterConfig> configs = semesterConfigRepository.findAllByTemplateId(templateId);
        for (ProfessorSurveySemesterConfig config : configs) {
            config.setTemplate(null);
            semesterConfigRepository.save(config);
        }

        templateQuestionRepository.deleteByTemplateId(templateId);
        templateRepository.delete(template);
    }

    @Override
    @Transactional
    public ProfessorSurveyCurrentConfigDTO applyTemplate(ProfessorSurveyApplyTemplateRequest request) throws Exception {
        String semester = normalizeRequiredPeriod(request.getSemester(), "El periodo es obligatorio");
        if (request.getTemplateId() == null) {
            throw new Exception("Debe indicar la plantilla");
        }

        ProfessorSurveyTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new Exception("Plantilla no encontrada"));

        List<Long> questionIds = normalizeQuestionIds(
            templateQuestionRepository.findByTemplateIdOrderByDisplayOrderAsc(template.getId()).stream()
                .map(item -> item.getQuestion().getId())
            .collect(Collectors.toList())
        );

        if (questionIds.isEmpty()) {
            throw new Exception("La plantilla seleccionada no tiene preguntas");
        }

        ProfessorSurveyCurrentConfigRequest configRequest = new ProfessorSurveyCurrentConfigRequest();
        configRequest.setSemester(semester);
        configRequest.setQuestionIds(questionIds);
        ProfessorSurveyCurrentConfigDTO config = saveCurrentConfig(configRequest);

        semesterConfigRepository.findBySemester(semester).ifPresent(saved -> {
            saved.setTemplate(template);
            semesterConfigRepository.save(saved);
        });

        return config;
    }

    private Optional<ProfessorSurveySemesterConfig> resolveConfig(String semester) {
        if (semester != null && !semester.isBlank()) {
            String requestedPeriod = semester.trim();
            if (isValidPeriod(requestedPeriod)) {
                Optional<ProfessorSurveySemesterConfig> exactConfig = semesterConfigRepository.findBySemester(requestedPeriod);
                if (exactConfig.isPresent()) {
                    return exactConfig;
                }
            }
        }

        Optional<ProfessorSurveySemesterConfig> activeConfig = semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc();
        if (activeConfig.isPresent() && isValidPeriod(activeConfig.get().getSemester())) {
            return activeConfig;
        }

        Optional<ProfessorSurveySemesterConfig> latestValidConfig = semesterConfigRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(config -> isValidPeriod(config.getSemester()))
                .findFirst();

        if (latestValidConfig.isPresent()) {
            return latestValidConfig;
        }

        return activeConfig;
    }

    private Integer getDisplayOrderInCurrentConfig(Long questionId, String semester) {
        Optional<ProfessorSurveySemesterConfig> configOpt = resolveConfig(semester);
        if (configOpt.isEmpty()) {
            return null;
        }
        return semesterQuestionRepository.findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(configOpt.get().getId()).stream()
                .filter(item -> item.getQuestion() != null && Objects.equals(item.getQuestion().getId(), questionId))
                .map(ProfessorSurveySemesterQuestion::getDisplayOrder)
                .findFirst()
                .orElse(null);
    }

    private String resolveSemesterForEdition(String semester) {
        if (semester != null && !semester.isBlank()) {
            String requestedPeriod = semester.trim();
            return isValidPeriod(requestedPeriod) ? requestedPeriod : null;
        }
        return resolveConfig(null)
                .map(ProfessorSurveySemesterConfig::getSemester)
                .map(this::normalizeOptionalPeriod)
                .orElse(resolveCurrentPeriod());
    }

    private String resolveCurrentPeriod() {
        LocalDate now = LocalDate.now();
        int period = now.getMonthValue() <= 6 ? 1 : 2;
        return String.format(Locale.ROOT, "%04d-%d", now.getYear(), period);
    }

    private String normalizeOptionalPeriod(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return isValidPeriod(normalized) ? normalized : null;
    }

    private boolean isValidPeriod(String value) {
        return value != null && value.matches(PERIOD_REGEX);
    }

    private List<Long> normalizeQuestionIds(List<Long> rawQuestionIds) {
        return Optional.ofNullable(rawQuestionIds).orElse(Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private ProfessorSurveyQuestionDTO toQuestionDTO(ProfessorSurveyQuestion question, Integer displayOrder) {
        ProfessorSurveyQuestionDTO dto = new ProfessorSurveyQuestionDTO();
        dto.setId(question.getId());
        dto.setQuestionKey(question.getQuestionKey());
        dto.setStatement(question.getStatement());
        dto.setCategory(question.getCategory());
        dto.setBankActive(question.isBankActive());
        dto.setDisplayOrder(displayOrder);
        dto.setSelectedInCurrentSurvey(displayOrder != null);
        return dto;
    }

    private ProfessorSurveyTemplateDTO toTemplateDTO(ProfessorSurveyTemplate template) {
        ProfessorSurveyTemplateDTO dto = new ProfessorSurveyTemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setCreatedForSemester(normalizeOptionalPeriod(template.getCreatedForSemester()));

        List<ProfessorSurveyQuestionDTO> questions = templateQuestionRepository.findByTemplateIdOrderByDisplayOrderAsc(template.getId()).stream()
                .filter(item -> item.getQuestion() != null)
                .map(item -> toQuestionDTO(item.getQuestion(), item.getDisplayOrder()))
                .collect(Collectors.toList());
        dto.setQuestions(questions);
        return dto;
    }

    private String normalizeRequired(String value, String errorMessage) throws Exception {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new Exception(errorMessage);
        }
        return normalized;
    }

    private String normalizeRequiredPeriod(String value, String requiredErrorMessage) throws Exception {
        String normalized = normalizeRequired(value, requiredErrorMessage);
        if (!isValidPeriod(normalized)) {
            throw new Exception("El periodo debe tener formato AAAA-1 o AAAA-2");
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

