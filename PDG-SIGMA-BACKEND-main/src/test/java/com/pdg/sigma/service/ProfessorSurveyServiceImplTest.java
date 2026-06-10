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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessorSurveyServiceImplTest {

    @Mock
    private ProfessorSurveyQuestionRepository questionRepository;

    @Mock
    private ProfessorSurveySemesterConfigRepository semesterConfigRepository;

    @Mock
    private ProfessorSurveySemesterQuestionRepository semesterQuestionRepository;

    @Mock
    private ProfessorSurveyTemplateRepository templateRepository;

    @Mock
    private ProfessorSurveyTemplateQuestionRepository templateQuestionRepository;

    @Mock
    private SupervisorEvaluationAnswerRepository supervisorEvaluationAnswerRepository;

    @InjectMocks
    private ProfessorSurveyServiceImpl service;

    @Captor
    private ArgumentCaptor<ProfessorSurveyQuestion> questionCaptor;

    private ProfessorSurveyQuestion sampleQuestion;
    private ProfessorSurveySemesterConfig sampleConfig;
    private ProfessorSurveySemesterQuestion sampleSemesterQuestion;

    @BeforeEach
    void setUp() {
        sampleQuestion = new ProfessorSurveyQuestion();
        sampleQuestion.setId(1L);
        sampleQuestion.setQuestionKey("test_key");
        sampleQuestion.setStatement("Test statement");
        sampleQuestion.setCategory("Test category");
        sampleQuestion.setBankActive(true);

        sampleConfig = new ProfessorSurveySemesterConfig();
        sampleConfig.setId(100L);
        sampleConfig.setSemester("2026-1");
        sampleConfig.setActive(true);

        sampleSemesterQuestion = new ProfessorSurveySemesterQuestion();
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

        List<ProfessorSurveyQuestionDTO> result = service.getQuestionBank("2026-1");

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getDisplayOrder());
    }

    @Test
    @DisplayName("Should create question successfully")
    void createQuestion_success() throws Exception {
        ProfessorSurveyQuestionCreateRequest request = new ProfessorSurveyQuestionCreateRequest();
        request.setStatement("New question");
        request.setCategory("New category");

        when(questionRepository.save(any(ProfessorSurveyQuestion.class))).thenAnswer(i -> {
            ProfessorSurveyQuestion q = i.getArgument(0);
            q.setId(2L);
            return q;
        });

        ProfessorSurveyQuestionDTO result = service.createQuestion(request);

        assertNotNull(result);
        assertEquals("New question", result.getStatement());
        assertTrue(result.isBankActive());
        verify(questionRepository).save(questionCaptor.capture());
        assertTrue(questionCaptor.getValue().getQuestionKey().startsWith("new_category_new_question"));
    }

    @Test
    @DisplayName("Should throw when creating question with null statement")
    void createQuestion_nullStatement_throws() {
        ProfessorSurveyQuestionCreateRequest request = new ProfessorSurveyQuestionCreateRequest();
        request.setCategory("Category");

        Exception ex = assertThrows(Exception.class, () -> service.createQuestion(request));
        assertEquals("El texto de la pregunta es obligatorio", ex.getMessage());
    }

    @Test
    @DisplayName("Should update question successfully")
    void updateQuestion_success() throws Exception {
        ProfessorSurveyQuestionUpdateRequest request = new ProfessorSurveyQuestionUpdateRequest();
        request.setStatement("Updated");
        request.setCategory("Updated cat");

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(questionRepository.save(any(ProfessorSurveyQuestion.class))).thenReturn(sampleQuestion);

        ProfessorSurveyQuestionDTO result = service.updateQuestion(1L, request, "2026-1");

        assertEquals("Updated", result.getStatement());
        assertEquals("Updated cat", result.getCategory());
    }

    @Test
    @DisplayName("Should throw when updating non-existent question")
    void updateQuestion_notFound_throws() {
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        ProfessorSurveyQuestionUpdateRequest request = new ProfessorSurveyQuestionUpdateRequest();
        Exception ex = assertThrows(Exception.class, () -> service.updateQuestion(99L, request, "2026-1"));
        assertEquals("Pregunta no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when updating question with existing evaluations")
    void updateQuestion_withExistingResponses_throws() {
        ProfessorSurveyQuestionUpdateRequest request = new ProfessorSurveyQuestionUpdateRequest();
        request.setStatement("S");
        request.setCategory("C");

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(supervisorEvaluationAnswerRepository.existsByQuestionIdAndEvaluationSemester(1L, "2026-1")).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> service.updateQuestion(1L, request, "2026-1"));
        assertEquals("No se puede editar la pregunta porque ya tiene respuestas asociadas en el periodo actual", ex.getMessage());
    }

    @Test
    @DisplayName("Should deactivate question and reorder")
    void updateQuestionStatus_deactivate_success() throws Exception {
        ProfessorSurveySemesterQuestion other = new ProfessorSurveySemesterQuestion();
        other.setId(11L);
        other.setSemesterConfig(sampleConfig);
        other.setActive(true);
        other.setDisplayOrder(2);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()).thenReturn(Optional.of(sampleConfig));
        when(semesterQuestionRepository.findBySemesterConfigIdOrderByDisplayOrderAsc(100L))
                .thenReturn(List.of(sampleSemesterQuestion, other));
        when(questionRepository.save(any(ProfessorSurveyQuestion.class))).thenReturn(sampleQuestion);

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
        when(questionRepository.save(any(ProfessorSurveyQuestion.class))).thenReturn(sampleQuestion);

        ProfessorSurveyQuestionDTO result = service.updateQuestionStatus(1L, true);

        assertTrue(result.isBankActive());
    }

    @Test
    @DisplayName("Should get current config when present")
    void getCurrentConfig_withConfig_returnsQuestions() {
        when(semesterConfigRepository.findBySemester("2026-1")).thenReturn(Optional.of(sampleConfig));
        when(semesterQuestionRepository.findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(100L))
                .thenReturn(List.of(sampleSemesterQuestion));

        ProfessorSurveyCurrentConfigDTO result = service.getCurrentConfig("2026-1");

        assertEquals("2026-1", result.getSemester());
        assertEquals(1, result.getQuestions().size());
    }

    @Test
    @DisplayName("Should get current config with resolved period when empty")
    void getCurrentConfig_noConfig_resolvesPeriod() {
        int year = LocalDate.now().getYear();
        int period = LocalDate.now().getMonthValue() <= 6 ? 1 : 2;
        String expectedSemester = String.format("%04d-%d", year, period);

        when(semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(semesterConfigRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(new ArrayList<>());

        ProfessorSurveyCurrentConfigDTO result = service.getCurrentConfig(null);

        assertEquals(expectedSemester, result.getSemester());
        assertTrue(result.getQuestions().isEmpty());
    }

    @Test
    @DisplayName("Should save current config successfully")
    void saveCurrentConfig_success() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        String semester = currentYear + "-1";

        ProfessorSurveyCurrentConfigRequest request = new ProfessorSurveyCurrentConfigRequest();
        request.setSemester(semester);
        request.setQuestionIds(List.of(1L));

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(semesterConfigRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(new ArrayList<>());
        when(semesterConfigRepository.findBySemester(semester)).thenReturn(Optional.empty());
        when(semesterConfigRepository.save(any(ProfessorSurveySemesterConfig.class))).thenAnswer(i -> {
            ProfessorSurveySemesterConfig c = i.getArgument(0);
            c.setId(200L);
            return c;
        });

        service.saveCurrentConfig(request);

        verify(semesterQuestionRepository).deleteBySemesterConfigId(200L);
        verify(semesterQuestionRepository).save(any(ProfessorSurveySemesterQuestion.class));
    }

    @Test
    @DisplayName("Should throw when saving config with empty questions")
    void saveCurrentConfig_emptyQuestions_throws() {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        ProfessorSurveyCurrentConfigRequest request = new ProfessorSurveyCurrentConfigRequest();
        request.setSemester(currentYear + "-1");
        request.setQuestionIds(Collections.emptyList());

        Exception ex = assertThrows(Exception.class, () -> service.saveCurrentConfig(request));
        assertEquals("Debe seleccionar al menos una pregunta para la encuesta activa", ex.getMessage());
    }

    @Test
    @DisplayName("Should list templates")
    void listTemplates_returnsAll() {
        ProfessorSurveyTemplate template = new ProfessorSurveyTemplate();
        template.setId(1L);
        template.setName("Template 1");
        template.setCreatedForSemester("2026-1");

        when(templateRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(template));

        List<ProfessorSurveyTemplateDTO> result = service.listTemplates();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should create template successfully")
    void createTemplate_success() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        ProfessorSurveyTemplateCreateRequest request = new ProfessorSurveyTemplateCreateRequest();
        request.setName("Template");
        request.setCreatedForSemester(currentYear + "-1");
        request.setQuestionIds(List.of(1L));

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(templateRepository.save(any(ProfessorSurveyTemplate.class))).thenAnswer(i -> {
            ProfessorSurveyTemplate t = i.getArgument(0);
            t.setId(1L);
            return t;
        });

        ProfessorSurveyTemplateDTO result = service.createTemplate(request);

        assertNotNull(result);
        verify(templateQuestionRepository).save(any(ProfessorSurveyTemplateQuestion.class));
    }

    @Test
    @DisplayName("Should update template successfully")
    void updateTemplate_success() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        ProfessorSurveyTemplate template = new ProfessorSurveyTemplate();
        template.setId(1L);
        template.setName("Original");

        ProfessorSurveyTemplateUpdateRequest request = new ProfessorSurveyTemplateUpdateRequest();
        request.setName("Updated");
        request.setCreatedForSemester(currentYear + "-1");
        request.setQuestionIds(List.of(1L));

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(templateRepository.save(any(ProfessorSurveyTemplate.class))).thenReturn(template);

        ProfessorSurveyTemplateDTO result = service.updateTemplate(1L, request);

        assertNotNull(result);
        verify(templateQuestionRepository).deleteByTemplateId(1L);
    }

    @Test
    @DisplayName("Should delete template successfully")
    void deleteTemplate_success() throws Exception {
        ProfessorSurveyTemplate template = new ProfessorSurveyTemplate();
        template.setId(1L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(semesterConfigRepository.findAllByTemplateId(1L)).thenReturn(new ArrayList<>());

        service.deleteTemplate(1L);

        verify(templateQuestionRepository).deleteByTemplateId(1L);
        verify(templateRepository).delete(template);
    }

    @Test
    @DisplayName("Should apply template successfully")
    void applyTemplate_success() throws Exception {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        ProfessorSurveyTemplate template = new ProfessorSurveyTemplate();
        template.setId(1L);

        ProfessorSurveyTemplateQuestion tq = new ProfessorSurveyTemplateQuestion();
        tq.setQuestion(sampleQuestion);

        ProfessorSurveyApplyTemplateRequest request = new ProfessorSurveyApplyTemplateRequest();
        request.setSemester(currentYear + "-1");
        request.setTemplateId(1L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateQuestionRepository.findByTemplateIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(tq));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(semesterConfigRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(new ArrayList<>());
        when(semesterConfigRepository.findBySemester(currentYear + "-1")).thenReturn(Optional.of(sampleConfig));
        when(semesterConfigRepository.save(any(ProfessorSurveySemesterConfig.class))).thenReturn(sampleConfig);

        ProfessorSurveyCurrentConfigDTO result = service.applyTemplate(request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw when applying template with null ID")
    void applyTemplate_nullTemplateId_throws() {
        ProfessorSurveyApplyTemplateRequest request = new ProfessorSurveyApplyTemplateRequest();
        request.setSemester("2026-1");

        Exception ex = assertThrows(Exception.class, () -> service.applyTemplate(request));
        assertEquals("Debe indicar la plantilla", ex.getMessage());
    }

    @Test
    @DisplayName("Should not fail when no active config and no semester")
    void resolveConfig_noSemester_noActive_returnsEmpty() {
        when(semesterConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(semesterConfigRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(new ArrayList<>());

        ProfessorSurveyCurrentConfigDTO result = service.getCurrentConfig(null);

        assertTrue(result.getQuestions().isEmpty());
    }
}
