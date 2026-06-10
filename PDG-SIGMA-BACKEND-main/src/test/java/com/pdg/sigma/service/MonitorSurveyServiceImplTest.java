package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorSurveyServiceImplTest {

    @Mock
    private MonitorSurveyQuestionRepository questionRepository;

    @Mock
    private MonitorSurveySemesterConfigRepository semesterConfigRepository;

    @Mock
    private MonitorSurveySemesterQuestionRepository semesterQuestionRepository;

    @Mock
    private MonitorSurveyTemplateRepository templateRepository;

    @Mock
    private MonitorSurveyTemplateQuestionRepository templateQuestionRepository;

    @Mock
    private MonitorSurveyResponseRepository responseRepository;

    @Mock
    private MonitorSurveyResponseAnswerRepository responseAnswerRepository;

    @Mock
    private MonitorSurveyIntegrationConfigRepository integrationConfigRepository;

    @InjectMocks
    private MonitorSurveyServiceImpl service;

    @Captor
    private ArgumentCaptor<MonitorSurveyQuestion> questionCaptor;

    @Captor
    private ArgumentCaptor<MonitorSurveySemesterConfig> configCaptor;

    @Captor
    private ArgumentCaptor<MonitorSurveySemesterQuestion> semesterQuestionCaptor;

    private MonitorSurveyQuestion sampleQuestion;
    private MonitorSurveySemesterConfig sampleConfig;
    private MonitorSurveySemesterQuestion sampleSemesterQuestion;

    @BeforeEach
    void setUp() {
        sampleQuestion = new MonitorSurveyQuestion();
        sampleQuestion.setId(1L);
        sampleQuestion.setQuestionKey("test_key");
        sampleQuestion.setStatement("Test statement");
        sampleQuestion.setCategory("Test category");
        sampleQuestion.setBankActive(true);

        sampleConfig = new MonitorSurveySemesterConfig();
        sampleConfig.setId(100L);
        sampleConfig.setSemester("2026-1");
        sampleConfig.setActive(true);

        sampleSemesterQuestion = new MonitorSurveySemesterQuestion();
        sampleSemesterQuestion.setId(10L);
        sampleSemesterQuestion.setSemesterConfig(sampleConfig);
        sampleSemesterQuestion.setQuestion(sampleQuestion);
        sampleSemesterQuestion.setDisplayOrder(1);
        sampleSemesterQuestion.setActive(true);
    }

    @Test
    @DisplayName("Should get question bank with display orders")
    void getQuestionBank_withConfig_returnsMappedQuestions() {
        when(semesterConfigRepository.findBySemester("2026-1")).thenReturn(Optional.of(sampleConfig));
        when(semesterQuestionRepository.findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(100L))
                .thenReturn(List.of(sampleSemesterQuestion));
        when(questionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleQuestion));

        List<MonitorSurveyQuestionDTO> result = service.getQuestionBank("2026-1");

        assertEquals(1, result.size());
        assertEquals("Test statement", result.get(0).getStatement());
        assertEquals(Integer.valueOf(1), result.get(0).getDisplayOrder());
        assertTrue(result.get(0).isSelectedInCurrentSurvey());
    }

    @Test
    @DisplayName("Should get question bank with null display order when no config")
    void getQuestionBank_withoutConfig_returnsNullDisplayOrder() {
        when(semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(questionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleQuestion));

        List<MonitorSurveyQuestionDTO> result = service.getQuestionBank(null);

        assertEquals(1, result.size());
        assertNull(result.get(0).getDisplayOrder());
        assertFalse(result.get(0).isSelectedInCurrentSurvey());
    }

    @Test
    @DisplayName("Should create question successfully")
    void createQuestion_success() throws Exception {
        MonitorSurveyQuestionCreateRequest request = new MonitorSurveyQuestionCreateRequest();
        request.setStatement("New question");
        request.setCategory("New category");

        when(questionRepository.save(any(MonitorSurveyQuestion.class))).thenAnswer(i -> {
            MonitorSurveyQuestion q = i.getArgument(0);
            q.setId(2L);
            return q;
        });

        MonitorSurveyQuestionDTO result = service.createQuestion(request);

        assertNotNull(result);
        assertEquals("New question", result.getStatement());
        assertEquals("New category", result.getCategory());
        assertTrue(result.isBankActive());
        verify(questionRepository).save(questionCaptor.capture());
        assertTrue(questionCaptor.getValue().getQuestionKey().startsWith("new_category_new_question"));
    }

    @Test
    @DisplayName("Should throw when creating question with null statement")
    void createQuestion_nullStatement_throws() {
        MonitorSurveyQuestionCreateRequest request = new MonitorSurveyQuestionCreateRequest();
        request.setCategory("Category");

        Exception ex = assertThrows(Exception.class, () -> service.createQuestion(request));
        assertEquals("El texto de la pregunta es obligatorio", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when creating question with null category")
    void createQuestion_nullCategory_throws() {
        MonitorSurveyQuestionCreateRequest request = new MonitorSurveyQuestionCreateRequest();
        request.setStatement("Statement");

        Exception ex = assertThrows(Exception.class, () -> service.createQuestion(request));
        assertEquals("La categoría es obligatoria", ex.getMessage());
    }

    @Test
    @DisplayName("Should update question successfully")
    void updateQuestion_success() throws Exception {
        MonitorSurveyQuestionUpdateRequest request = new MonitorSurveyQuestionUpdateRequest();
        request.setStatement("Updated statement");
        request.setCategory("Updated category");

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(questionRepository.save(any(MonitorSurveyQuestion.class))).thenReturn(sampleQuestion);

        MonitorSurveyQuestionDTO result = service.updateQuestion(1L, request, "2026-1");

        assertEquals("Updated statement", result.getStatement());
        assertEquals("Updated category", result.getCategory());
        verify(questionRepository).save(questionCaptor.capture());
        assertEquals("Updated statement", questionCaptor.getValue().getStatement());
    }

    @Test
    @DisplayName("Should throw when updating non-existent question")
    void updateQuestion_notFound_throws() {
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        MonitorSurveyQuestionUpdateRequest request = new MonitorSurveyQuestionUpdateRequest();
        Exception ex = assertThrows(Exception.class, () -> service.updateQuestion(99L, request, "2026-1"));
        assertEquals("Pregunta no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when updating question with existing responses")
    void updateQuestion_withExistingResponses_throws() {
        MonitorSurveyQuestionUpdateRequest request = new MonitorSurveyQuestionUpdateRequest();
        request.setStatement("S");
        request.setCategory("C");

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(responseAnswerRepository.existsByQuestionIdAndResponseSemester(1L, "2026-1")).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> service.updateQuestion(1L, request, "2026-1"));
        assertEquals("No se puede editar la pregunta porque ya tiene respuestas asociadas en el semestre actual", ex.getMessage());
    }

    @Test
    @DisplayName("Should deactivate question and reorder")
    void updateQuestionStatus_deactivate_success() throws Exception {
        MonitorSurveySemesterQuestion other = new MonitorSurveySemesterQuestion();
        other.setId(11L);
        other.setSemesterConfig(sampleConfig);
        other.setActive(true);
        other.setDisplayOrder(2);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()).thenReturn(Optional.of(sampleConfig));
        when(semesterQuestionRepository.findBySemesterConfigIdOrderByDisplayOrderAsc(100L))
                .thenReturn(List.of(sampleSemesterQuestion, other));
        when(questionRepository.save(any(MonitorSurveyQuestion.class))).thenReturn(sampleQuestion);

        service.updateQuestionStatus(1L, false);

        assertFalse(sampleQuestion.isBankActive());
        assertFalse(sampleSemesterQuestion.isActive());
        verify(semesterQuestionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should activate question status")
    void updateQuestionStatus_activate_success() throws Exception {
        sampleQuestion.setBankActive(false);
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(questionRepository.save(any(MonitorSurveyQuestion.class))).thenReturn(sampleQuestion);

        MonitorSurveyQuestionDTO result = service.updateQuestionStatus(1L, true);

        assertTrue(result.isBankActive());
    }

    @Test
    @DisplayName("Should get current config when present")
    void getCurrentConfig_withConfig_returnsQuestions() {
        when(semesterConfigRepository.findBySemester("2026-1")).thenReturn(Optional.of(sampleConfig));
        when(semesterQuestionRepository.findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(100L))
                .thenReturn(List.of(sampleSemesterQuestion));

        MonitorSurveyCurrentConfigDTO result = service.getCurrentConfig("2026-1");

        assertEquals("2026-1", result.getSemester());
        assertEquals(1, result.getQuestions().size());
    }

    @Test
    @DisplayName("Should get current config with empty questions when no config")
    void getCurrentConfig_noConfig_returnsEmptyQuestions() {
        when(semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

        MonitorSurveyCurrentConfigDTO result = service.getCurrentConfig(null);

        assertTrue(result.getQuestions().isEmpty());
    }

    @Test
    @DisplayName("Should save current config successfully")
    void saveCurrentConfig_success() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        String semester = currentYear + "-1";

        MonitorSurveyCurrentConfigRequest request = new MonitorSurveyCurrentConfigRequest();
        request.setSemester(semester);
        request.setQuestionIds(List.of(1L));

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(semesterConfigRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(new ArrayList<>());
        when(semesterConfigRepository.findBySemester(semester)).thenReturn(Optional.empty());
        when(semesterConfigRepository.save(any(MonitorSurveySemesterConfig.class))).thenAnswer(i -> {
            MonitorSurveySemesterConfig c = i.getArgument(0);
            c.setId(200L);
            return c;
        });

        service.saveCurrentConfig(request);

        verify(semesterQuestionRepository).deleteBySemesterConfigId(200L);
        verify(semesterQuestionRepository).save(any(MonitorSurveySemesterQuestion.class));
    }

    @Test
    @DisplayName("Should throw when saving config with empty questions")
    void saveCurrentConfig_emptyQuestions_throws() {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        MonitorSurveyCurrentConfigRequest request = new MonitorSurveyCurrentConfigRequest();
        request.setSemester(currentYear + "-1");
        request.setQuestionIds(Collections.emptyList());

        Exception ex = assertThrows(Exception.class, () -> service.saveCurrentConfig(request));
        assertEquals("Debe seleccionar al menos una pregunta para la encuesta activa", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when saving config with inactive question")
    void saveCurrentConfig_inactiveQuestion_throws() {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        sampleQuestion.setBankActive(false);

        MonitorSurveyCurrentConfigRequest request = new MonitorSurveyCurrentConfigRequest();
        request.setSemester(currentYear + "-1");
        request.setQuestionIds(List.of(1L));

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));

        Exception ex = assertThrows(Exception.class, () -> service.saveCurrentConfig(request));
        assertTrue(ex.getMessage().contains("inactiva en el banco"));
    }

    @Test
    @DisplayName("Should list templates")
    void listTemplates_returnsAll() {
        MonitorSurveyTemplate template = new MonitorSurveyTemplate();
        template.setId(1L);
        template.setName("Template 1");

        when(templateRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(template));

        List<MonitorSurveyTemplateDTO> result = service.listTemplates();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should create template successfully")
    void createTemplate_success() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        MonitorSurveyTemplateCreateRequest request = new MonitorSurveyTemplateCreateRequest();
        request.setName("Template");
        request.setCreatedForSemester(currentYear + "-1");
        request.setQuestionIds(List.of(1L));
        request.setDescription("Desc");

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(templateRepository.save(any(MonitorSurveyTemplate.class))).thenAnswer(i -> {
            MonitorSurveyTemplate t = i.getArgument(0);
            t.setId(1L);
            return t;
        });

        MonitorSurveyTemplateDTO result = service.createTemplate(request);

        assertNotNull(result);
        verify(templateQuestionRepository).save(any(MonitorSurveyTemplateQuestion.class));
    }

    @Test
    @DisplayName("Should throw when creating template with empty question IDs")
    void createTemplate_emptyQuestions_throws() {
        MonitorSurveyTemplateCreateRequest request = new MonitorSurveyTemplateCreateRequest();
        request.setName("Template");
        request.setCreatedForSemester("2026-1");
        request.setQuestionIds(Collections.emptyList());

        Exception ex = assertThrows(Exception.class, () -> service.createTemplate(request));
        assertEquals("Debe seleccionar preguntas para crear la plantilla", ex.getMessage());
    }

    @Test
    @DisplayName("Should update template successfully")
    void updateTemplate_success() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        MonitorSurveyTemplate template = new MonitorSurveyTemplate();
        template.setId(1L);
        template.setName("Original");

        MonitorSurveyTemplateUpdateRequest request = new MonitorSurveyTemplateUpdateRequest();
        request.setName("Updated");
        request.setCreatedForSemester(currentYear + "-1");
        request.setQuestionIds(List.of(1L));

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(templateRepository.save(any(MonitorSurveyTemplate.class))).thenReturn(template);

        MonitorSurveyTemplateDTO result = service.updateTemplate(1L, request);

        assertNotNull(result);
        verify(templateQuestionRepository).deleteByTemplateId(1L);
    }

    @Test
    @DisplayName("Should delete template successfully")
    void deleteTemplate_success() throws Exception {
        MonitorSurveyTemplate template = new MonitorSurveyTemplate();
        template.setId(1L);
        template.setName("To delete");

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(semesterConfigRepository.findAllByTemplateId(1L)).thenReturn(new ArrayList<>());

        service.deleteTemplate(1L);

        verify(templateQuestionRepository).deleteByTemplateId(1L);
        verify(templateRepository).delete(template);
    }

    @Test
    @DisplayName("Should delete template and nullify config references")
    void deleteTemplate_withConfigs_nullifiesReference() throws Exception {
        MonitorSurveyTemplate template = new MonitorSurveyTemplate();
        template.setId(1L);

        MonitorSurveySemesterConfig config = new MonitorSurveySemesterConfig();
        config.setId(100L);
        config.setTemplate(template);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(semesterConfigRepository.findAllByTemplateId(1L)).thenReturn(List.of(config));

        service.deleteTemplate(1L);

        assertNull(config.getTemplate());
        verify(semesterConfigRepository).save(config);
    }

    @Test
    @DisplayName("Should throw when deleting non-existent template")
    void deleteTemplate_notFound_throws() {
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.deleteTemplate(99L));
        assertEquals("Plantilla no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("Should apply template successfully")
    void applyTemplate_success() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        MonitorSurveyTemplate template = new MonitorSurveyTemplate();
        template.setId(1L);
        template.setName("Template");

        MonitorSurveyTemplateQuestion tq = new MonitorSurveyTemplateQuestion();
        tq.setQuestion(sampleQuestion);

        MonitorSurveyApplyTemplateRequest request = new MonitorSurveyApplyTemplateRequest();
        request.setSemester(currentYear + "-1");
        request.setTemplateId(1L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateQuestionRepository.findByTemplateIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(tq));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(semesterConfigRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(new ArrayList<>());
        when(semesterConfigRepository.findBySemester(currentYear + "-1")).thenReturn(Optional.of(sampleConfig));
        when(semesterConfigRepository.save(any(MonitorSurveySemesterConfig.class))).thenReturn(sampleConfig);

        MonitorSurveyCurrentConfigDTO result = service.applyTemplate(request);

        assertNotNull(result);
        verify(semesterConfigRepository, times(2)).save(configCaptor.capture());
    }

    @Test
    @DisplayName("Should throw when applying template with null ID")
    void applyTemplate_nullTemplateId_throws() {
        MonitorSurveyApplyTemplateRequest request = new MonitorSurveyApplyTemplateRequest();
        request.setSemester("2026-1");

        Exception ex = assertThrows(Exception.class, () -> service.applyTemplate(request));
        assertEquals("Debe indicar la plantilla", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when applying template with empty questions")
    void applyTemplate_emptyTemplate_throws() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        MonitorSurveyTemplate template = new MonitorSurveyTemplate();
        template.setId(1L);

        MonitorSurveyApplyTemplateRequest request = new MonitorSurveyApplyTemplateRequest();
        request.setSemester(currentYear + "-1");
        request.setTemplateId(1L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateQuestionRepository.findByTemplateIdOrderByDisplayOrderAsc(1L)).thenReturn(new ArrayList<>());

        Exception ex = assertThrows(Exception.class, () -> service.applyTemplate(request));
        assertEquals("La plantilla seleccionada no tiene preguntas", ex.getMessage());
    }

    @Test
    @DisplayName("Should get public questions")
    void getPublicQuestions_returnsMapped() {
        when(semesterConfigRepository.findBySemester("2026-1")).thenReturn(Optional.of(sampleConfig));
        when(semesterQuestionRepository.findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(100L))
                .thenReturn(List.of(sampleSemesterQuestion));

        List<MonitorSurveyPublicQuestionDTO> result = service.getPublicQuestions("2026-1");

        assertEquals(1, result.size());
        assertEquals("test_key", result.get(0).getQuestionKey());
        assertEquals(Integer.valueOf(1), result.get(0).getDisplayOrder());
    }

    @Test
    @DisplayName("Should store public response successfully")
    void storePublicResponse_success() throws Exception {
        MonitorSurveyPublicResponseRequest request = new MonitorSurveyPublicResponseRequest();
        request.setSemester("2026-1");
        request.setMonitorCode("M001");
        request.setMonitorName("Monitor");
        request.setAverageScore(5.0);

        MonitorSurveyPublicResponseAnswerDTO answer = new MonitorSurveyPublicResponseAnswerDTO();
        answer.setQuestionId(1L);
        answer.setScore(5);
        request.setAnswers(List.of(answer));

        when(responseRepository.save(any(MonitorSurveyResponse.class))).thenAnswer(i -> {
            MonitorSurveyResponse r = i.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));

        service.storePublicResponse(request);

        verify(responseAnswerRepository).save(any(MonitorSurveyResponseAnswer.class));
    }

    @Test
    @DisplayName("Should throw when storing response with empty answers")
    void storePublicResponse_emptyAnswers_throws() {
        MonitorSurveyPublicResponseRequest request = new MonitorSurveyPublicResponseRequest();
        request.setSemester("2026-1");
        request.setAnswers(Collections.emptyList());

        Exception ex = assertThrows(Exception.class, () -> service.storePublicResponse(request));
        assertEquals("Debe incluir respuestas para la encuesta", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when answer has null question ID")
    void storePublicResponse_nullQuestionId_throws() {
        MonitorSurveyPublicResponseRequest request = new MonitorSurveyPublicResponseRequest();
        request.setSemester("2026-1");

        MonitorSurveyPublicResponseAnswerDTO answer = new MonitorSurveyPublicResponseAnswerDTO();
        answer.setScore(5);
        request.setAnswers(List.of(answer));

        Exception ex = assertThrows(Exception.class, () -> service.storePublicResponse(request));
        assertEquals("Cada respuesta debe incluir pregunta y puntaje", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when answer score out of range")
    void storePublicResponse_scoreOutOfRange_throws() {
        MonitorSurveyPublicResponseRequest request = new MonitorSurveyPublicResponseRequest();
        request.setSemester("2026-1");

        MonitorSurveyPublicResponseAnswerDTO answer = new MonitorSurveyPublicResponseAnswerDTO();
        answer.setQuestionId(1L);
        answer.setScore(10);
        request.setAnswers(List.of(answer));

        Exception ex = assertThrows(Exception.class, () -> service.storePublicResponse(request));
        assertTrue(ex.getMessage().contains("Los puntajes deben estar entre"));
    }

    @Test
    @DisplayName("Should throw when question not found in response")
    void storePublicResponse_questionNotFound_throws() {
        MonitorSurveyPublicResponseRequest request = new MonitorSurveyPublicResponseRequest();
        request.setSemester("2026-1");

        MonitorSurveyPublicResponseAnswerDTO answer = new MonitorSurveyPublicResponseAnswerDTO();
        answer.setQuestionId(99L);
        answer.setScore(5);
        request.setAnswers(List.of(answer));

        when(responseRepository.save(any(MonitorSurveyResponse.class))).thenAnswer(i -> {
            MonitorSurveyResponse r = i.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.storePublicResponse(request));
        assertEquals("Pregunta no encontrada: 99", ex.getMessage());
    }

    @Test
    @DisplayName("Should get integration config when present")
    void getIntegrationConfig_returnsConfig() {
        MonitorSurveyIntegrationConfig config = new MonitorSurveyIntegrationConfig();
        config.setAppsScriptUrl("https://script.google.com");
        config.setDashboardUrl("https://dashboard.com");

        when(integrationConfigRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(config));

        MonitorSurveyIntegrationConfigDTO result = service.getIntegrationConfig();

        assertEquals("https://script.google.com", result.getAppsScriptUrl());
        assertEquals("https://dashboard.com", result.getDashboardUrl());
    }

    @Test
    @DisplayName("Should get integration config with nulls when empty")
    void getIntegrationConfig_noConfig_returnsNullUrls() {
        when(integrationConfigRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

        MonitorSurveyIntegrationConfigDTO result = service.getIntegrationConfig();

        assertNull(result.getAppsScriptUrl());
        assertNull(result.getDashboardUrl());
    }

    @Test
    @DisplayName("Should save integration config")
    void saveIntegrationConfig_success() throws Exception {
        MonitorSurveyIntegrationConfigRequest request = new MonitorSurveyIntegrationConfigRequest();
        request.setAppsScriptUrl("https://script.com");
        request.setDashboardUrl("https://dash.com");

        when(integrationConfigRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(integrationConfigRepository.save(any(MonitorSurveyIntegrationConfig.class))).thenAnswer(i -> i.getArgument(0));

        MonitorSurveyIntegrationConfigDTO result = service.saveIntegrationConfig(request);

        assertEquals("https://script.com", result.getAppsScriptUrl());
        assertEquals("https://dash.com", result.getDashboardUrl());
    }

    @Test
    @DisplayName("Should get survey report with data")
    void getSurveyReport_withData_returnsReport() {
        MonitorSurveyResponse response = new MonitorSurveyResponse();
        response.setId(1L);
        response.setSemester("2026-1");
        response.setAverageScore(4.5);

        MonitorSurveyResponseAnswer answer = new MonitorSurveyResponseAnswer();
        answer.setResponse(response);
        answer.setQuestion(sampleQuestion);
        answer.setScore(4);

        when(responseRepository.findByFilters("2026-1", null, null))
                .thenReturn(List.of(response));
        when(responseAnswerRepository.findByResponseIdInWithDetails(List.of(1L)))
                .thenReturn(List.of(answer));

        MonitorSurveyReportDTO result = service.getSurveyReport("2026-1", null, null);

        assertEquals("2026-1", result.getSemester());
        assertEquals(1, result.getTotalResponses());
        assertEquals(4.5, result.getAverageScore());
        assertEquals(1, result.getTotalAnswers());
        assertEquals(1, result.getQuestionStats().size());
    }

    @Test
    @DisplayName("Should get survey report with empty data")
    void getSurveyReport_noResponses_returnsEmpty() {
        when(responseRepository.findByFilters("2026-1", null, null))
                .thenReturn(Collections.emptyList());

        MonitorSurveyReportDTO result = service.getSurveyReport("2026-1", null, null);

        assertEquals("2026-1", result.getSemester());
        assertEquals(0, result.getTotalResponses());
        assertEquals(0.0, result.getAverageScore());
    }

    @Test
    @DisplayName("Should export CSV report")
    void exportSurveyReportCsv_returnsCsvString() {
        MonitorSurveyResponse response = new MonitorSurveyResponse();
        response.setId(1L);
        response.setSemester("2026-1");
        response.setAverageScore(4.5);

        MonitorSurveyResponseAnswer answer = new MonitorSurveyResponseAnswer();
        answer.setResponse(response);
        answer.setQuestion(sampleQuestion);
        answer.setScore(4);

        when(responseRepository.findByFilters("2026-1", null, null))
                .thenReturn(List.of(response));
        when(responseAnswerRepository.findByResponseIdInWithDetails(List.of(1L)))
                .thenReturn(List.of(answer));

        String csv = service.exportSurveyReportCsv("2026-1", null, null);

        assertTrue(csv.contains("response_id"));
        assertTrue(csv.contains("4.5"));
        assertTrue(csv.contains("4"));
    }

    @Test
    @DisplayName("Should export CSV with proper escaping")
    void exportSurveyReportCsv_escapesProperly() {
        MonitorSurveyResponse response = new MonitorSurveyResponse();
        response.setId(1L);
        response.setSemester("2026-1");
        response.setPositiveFeedback("Good, job");
        response.setImprovementFeedback("Needs \"work\"");

        MonitorSurveyResponseAnswer answer = new MonitorSurveyResponseAnswer();
        answer.setResponse(response);
        answer.setQuestion(sampleQuestion);
        answer.setScore(5);

        when(responseRepository.findByFilters("2026-1", null, null))
                .thenReturn(List.of(response));
        when(responseAnswerRepository.findByResponseIdInWithDetails(List.of(1L)))
                .thenReturn(List.of(answer));

        String csv = service.exportSurveyReportCsv("2026-1", null, null);

        assertTrue(csv.contains("\"Good, job\""));
        assertTrue(csv.contains("\"Needs \"\"work\"\"\""));
    }

    @Test
    @DisplayName("Should throw when creating question with invalid period format")
    void normalizeRequiredPeriod_invalidFormat_throws() {
        MonitorSurveyQuestionCreateRequest request = new MonitorSurveyQuestionCreateRequest();
        request.setStatement("Valid");
        request.setCategory("Valid");

        String pastYear = String.valueOf(LocalDate.now().getYear() - 1);
        String semester = pastYear + "-1";

        MonitorSurveyCurrentConfigRequest configRequest = new MonitorSurveyCurrentConfigRequest();
        configRequest.setSemester(semester);
        configRequest.setQuestionIds(List.of(1L));

        Exception ex = assertThrows(Exception.class, () -> service.saveCurrentConfig(configRequest));
        assertTrue(ex.getMessage().contains("debe corresponder al año actual") ||
                   ex.getMessage().contains("AAAA-1 o AAAA-2"));
    }
}
