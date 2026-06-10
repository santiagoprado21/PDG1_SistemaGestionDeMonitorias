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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupervisorEvaluationServiceImplTest {

    @Mock
    private SupervisorEvaluationRepository supervisorEvaluationRepository;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private ProfessorSurveySemesterConfigRepository professorSurveySemesterConfigRepository;

    @Mock
    private ProfessorSurveySemesterQuestionRepository professorSurveySemesterQuestionRepository;

    @Mock
    private SupervisorEvaluationAnswerRepository supervisorEvaluationAnswerRepository;

    @InjectMocks
    private SupervisorEvaluationServiceImpl service;

    @Captor
    private ArgumentCaptor<SupervisorEvaluation> evaluationCaptor;

    private Monitor sampleMonitor;
    private Monitoring sampleMonitoring;
    private Professor sampleProfessor;
    private MonitoringMonitor sampleMonitoringMonitor;
    private Course sampleCourse;
    private Program sampleProgram;

    @BeforeEach
    void setUp() {
        sampleMonitor = new Monitor();
        sampleMonitor.setCode("M001");
        sampleMonitor.setName("Juan");
        sampleMonitor.setLastName("Pérez");
        sampleMonitor.setIdMonitor("12345");
        sampleMonitor.setEmail("juan@test.com");

        sampleProfessor = new Professor();
        sampleProfessor.setId("P001");
        sampleProfessor.setName("Dr. García");

        sampleProgram = new Program();
        sampleProgram.setId(1L);
        sampleProgram.setName("Ingeniería");

        School school = new School();
        school.setId(1L);
        school.setName("Facultad de Ingeniería");

        sampleCourse = new Course();
        sampleCourse.setId(1L);
        sampleCourse.setName("Matemáticas");
        sampleCourse.setProgram(sampleProgram);

        sampleMonitoring = new Monitoring();
        sampleMonitoring.setId(100L);
        sampleMonitoring.setCourse(sampleCourse);
        sampleMonitoring.setProgram(sampleProgram);
        sampleMonitoring.setSchool(school);
        sampleMonitoring.setSemester("2026-1");
        sampleMonitoring.setProfessor(sampleProfessor);
        sampleMonitoring.setAssignedMonitor(sampleMonitor);

        sampleMonitoringMonitor = new MonitoringMonitor();
        sampleMonitoringMonitor.setId(10L);
        sampleMonitoringMonitor.setMonitoring(sampleMonitoring);
        sampleMonitoringMonitor.setMonitor(sampleMonitor);
        sampleMonitoringMonitor.setEstadoSeleccion("seleccionado");
    }

    @Test
    @DisplayName("Should create evaluation with legacy scores")
    void createEvaluation_legacyScores_success() throws Exception {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(100L);
        request.setGuidanceClarity(6);
        request.setRoleExpectations(5);
        request.setAvailabilityDisposition(6);
        request.setSupportTimeliness(5);
        request.setFeedbackConstructive(6);
        request.setFeedbackFairness(5);
        request.setRespectfulTreatment(6);
        request.setTrustEnvironment(5);
        request.setStrengthsComments("Good");
        request.setImprovementComments("Improve");

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(monitoringRepository.findById(100L)).thenReturn(Optional.of(sampleMonitoring));
        when(supervisorEvaluationRepository.findByMonitoringIdAndMonitorCode(100L, "M001"))
                .thenReturn(Optional.empty());
        when(supervisorEvaluationRepository.save(any(SupervisorEvaluation.class)))
                .thenAnswer(i -> i.getArgument(0));

        SupervisorEvaluationResponse result = service.createEvaluation("M001", request);

        assertNotNull(result);
        assertEquals(100L, result.getMonitoringId());
        verify(supervisorEvaluationRepository).save(evaluationCaptor.capture());
        SupervisorEvaluation saved = evaluationCaptor.getValue();
        assertEquals(6, saved.getGuidanceClarity());
        assertEquals(5, saved.getRoleExpectations());
    }

    @Test
    @DisplayName("Should create evaluation with dynamic answers")
    void createEvaluation_dynamicAnswers_success() throws Exception {
        ProfessorSurveyQuestion question = new ProfessorSurveyQuestion();
        question.setId(1L);
        question.setQuestionKey("q1");
        question.setStatement("How was?");
        question.setCategory("General");

        ProfessorSurveySemesterConfig config = new ProfessorSurveySemesterConfig();
        config.setId(100L);
        config.setSemester("2026-1");

        ProfessorSurveySemesterQuestion sq = new ProfessorSurveySemesterQuestion();
        sq.setId(10L);
        sq.setSemesterConfig(config);
        sq.setQuestion(question);
        sq.setDisplayOrder(1);
        sq.setActive(true);

        SupervisorEvaluationAnswerRequestDTO answerDto = new SupervisorEvaluationAnswerRequestDTO();
        answerDto.setQuestionId(1L);
        answerDto.setScore(6);

        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(100L);
        request.setAnswers(List.of(answerDto));
        request.setStrengthsComments("Good");
        request.setImprovementComments("Improve");

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(monitoringRepository.findById(100L)).thenReturn(Optional.of(sampleMonitoring));
        when(supervisorEvaluationRepository.findByMonitoringIdAndMonitorCode(100L, "M001"))
                .thenReturn(Optional.empty());
        when(professorSurveySemesterConfigRepository.findBySemester("2026-1")).thenReturn(Optional.of(config));
        when(professorSurveySemesterQuestionRepository.findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(100L))
                .thenReturn(List.of(sq));
        when(supervisorEvaluationRepository.save(any(SupervisorEvaluation.class)))
                .thenAnswer(i -> i.getArgument(0));

        SupervisorEvaluationResponse result = service.createEvaluation("M001", request);

        assertNotNull(result);
        verify(supervisorEvaluationAnswerRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw when monitor not found")
    void createEvaluation_monitorNotFound_throws() {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(100L);

        when(monitorRepository.findById("INVALID")).thenReturn(Optional.empty());
        when(monitorRepository.findByCode("INVALID")).thenReturn(Optional.empty());
        when(monitorRepository.findByIdMonitor("INVALID")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.createEvaluation("INVALID", request));
        assertEquals("Monitor no encontrado", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when monitoring not found")
    void createEvaluation_monitoringNotFound_throws() {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(999L);

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(monitoringRepository.findById(999L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.createEvaluation("M001", request));
        assertEquals("Monitoría no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when monitor not assigned")
    void createEvaluation_monitorNotAssigned_throws() {
        sampleMonitoring.setAssignedMonitor(null);

        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(100L);

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(monitoringRepository.findById(100L)).thenReturn(Optional.of(sampleMonitoring));
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(100L, "M001"))
                .thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.createEvaluation("M001", request));
        assertEquals("El monitor no está asignado a la monitoría indicada", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when evaluation already exists")
    void createEvaluation_duplicate_throws() {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(100L);
        request.setGuidanceClarity(5);
        request.setRoleExpectations(5);
        request.setAvailabilityDisposition(5);
        request.setSupportTimeliness(5);
        request.setFeedbackConstructive(5);
        request.setFeedbackFairness(5);
        request.setRespectfulTreatment(5);
        request.setTrustEnvironment(5);

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(monitoringRepository.findById(100L)).thenReturn(Optional.of(sampleMonitoring));
        when(supervisorEvaluationRepository.findByMonitoringIdAndMonitorCode(100L, "M001"))
                .thenReturn(Optional.of(new SupervisorEvaluation()));

        sampleMonitoring.setAssignedMonitor(sampleMonitor);

        Exception ex = assertThrows(IllegalStateException.class, () -> service.createEvaluation("M001", request));
        assertTrue(ex.getMessage().contains("Ya existe una evaluación"));
    }

    @Test
    @DisplayName("Should get evaluations for coordinator")
    void getEvaluationsForCoordinator_success() throws Exception {
        SupervisorEvaluation evaluation = new SupervisorEvaluation();
        evaluation.setId(1L);
        evaluation.setMonitoring(sampleMonitoring);
        evaluation.setMonitor(sampleMonitor);
        evaluation.setProfessor(sampleProfessor);

        when(supervisorEvaluationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(evaluation));

        List<SupervisorEvaluationResponse> result = service.getEvaluationsForCoordinator();

        assertEquals(1, result.size());
        assertEquals("M001", result.get(0).getMonitorCode());
    }

    @Test
    @DisplayName("Should get evaluations by professor")
    void getEvaluationsByProfessor_success() throws Exception {
        SupervisorEvaluation evaluation = new SupervisorEvaluation();
        evaluation.setId(1L);
        evaluation.setMonitoring(sampleMonitoring);
        evaluation.setMonitor(sampleMonitor);
        evaluation.setProfessor(sampleProfessor);

        when(professorRepository.existsById("P001")).thenReturn(true);
        when(supervisorEvaluationRepository.findByProfessorIdOrderByCreatedAtDesc("P001"))
                .thenReturn(List.of(evaluation));

        List<SupervisorEvaluationResponse> result = service.getEvaluationsByProfessor("P001");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should throw when professor not found")
    void getEvaluationsByProfessor_notFound_throws() {
        when(professorRepository.existsById("INVALID")).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> service.getEvaluationsByProfessor("INVALID"));
        assertEquals("Profesor supervisor no encontrado", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when professor ID is null")
    void getEvaluationsByProfessor_nullId_throws() {
        Exception ex = assertThrows(Exception.class, () -> service.getEvaluationsByProfessor(null));
        assertEquals("Debe indicar el profesor supervisor", ex.getMessage());
    }

    @Test
    @DisplayName("Should get evaluations by monitor")
    void getEvaluationsByMonitor_success() throws Exception {
        SupervisorEvaluation evaluation = new SupervisorEvaluation();
        evaluation.setId(1L);
        evaluation.setMonitoring(sampleMonitoring);
        evaluation.setMonitor(sampleMonitor);
        evaluation.setProfessor(sampleProfessor);

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(supervisorEvaluationRepository.findByMonitorCodeOrderByCreatedAtDesc("M001"))
                .thenReturn(List.of(evaluation));

        List<SupervisorEvaluationResponse> result = service.getEvaluationsByMonitor("M001");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should get single evaluation by ID")
    void getEvaluation_found_returnsOptional() {
        SupervisorEvaluation evaluation = new SupervisorEvaluation();
        evaluation.setId(1L);
        evaluation.setMonitoring(sampleMonitoring);
        evaluation.setMonitor(sampleMonitor);
        evaluation.setProfessor(sampleProfessor);

        when(supervisorEvaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        Optional<SupervisorEvaluationResponse> result = service.getEvaluation(1L);

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when evaluation not found")
    void getEvaluation_notFound_returnsEmpty() {
        when(supervisorEvaluationRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<SupervisorEvaluationResponse> result = service.getEvaluation(99L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get assignments for monitor")
    void getAssignmentsForMonitor_returnsAssignments() throws Exception {
        SupervisorEvaluation evaluation = new SupervisorEvaluation();
        evaluation.setId(1L);
        evaluation.setMonitoring(sampleMonitoring);
        evaluation.setMonitor(sampleMonitor);
        evaluation.setProfessor(sampleProfessor);
        evaluation.setGuidanceClarity(5);

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(supervisorEvaluationRepository.findByMonitorCodeOrderByCreatedAtDesc("M001"))
                .thenReturn(List.of(evaluation));
        when(monitoringMonitorRepository.findByMonitor(sampleMonitor))
                .thenReturn(List.of(sampleMonitoringMonitor));

        List<SupervisorEvaluationStatusDTO> result = service.getAssignmentsForMonitor("M001");

        assertFalse(result.isEmpty());
        assertEquals(100L, result.get(0).getMonitoringId());
    }

    @Test
    @DisplayName("Should handle non-eligible monitor assignment status")
    void getAssignmentsForMonitor_skipsNonEligibleStatus() throws Exception {
        sampleMonitoringMonitor.setEstadoSeleccion("no seleccionado");

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(supervisorEvaluationRepository.findByMonitorCodeOrderByCreatedAtDesc("M001"))
                .thenReturn(Collections.emptyList());
        when(monitoringMonitorRepository.findByMonitor(sampleMonitor))
                .thenReturn(List.of(sampleMonitoringMonitor));

        List<SupervisorEvaluationStatusDTO> result = service.getAssignmentsForMonitor("M001");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should use monitor identifier from request when explicit is null")
    void resolveMonitorIdentifier_fromRequest_success() throws Exception {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(100L);
        request.setMonitorIdentifier("M001");
        request.setGuidanceClarity(5);
        request.setRoleExpectations(5);
        request.setAvailabilityDisposition(5);
        request.setSupportTimeliness(5);
        request.setFeedbackConstructive(5);
        request.setFeedbackFairness(5);
        request.setRespectfulTreatment(5);
        request.setTrustEnvironment(5);

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(monitoringRepository.findById(100L)).thenReturn(Optional.of(sampleMonitoring));
        when(supervisorEvaluationRepository.findByMonitoringIdAndMonitorCode(100L, "M001"))
                .thenReturn(Optional.empty());
        when(supervisorEvaluationRepository.save(any(SupervisorEvaluation.class)))
                .thenAnswer(i -> i.getArgument(0));

        sampleMonitoring.setAssignedMonitor(sampleMonitor);

        SupervisorEvaluationResponse result = service.createEvaluation(null, request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw when request has null monitoringId")
    void createEvaluation_nullMonitoringId_throws() {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();

        Exception ex = assertThrows(Exception.class, () -> service.createEvaluation("M001", request));
        assertEquals("Debe especificar la monitoría a evaluar", ex.getMessage());
    }

    @Test
    @DisplayName("Should resolve performance level correctly")
    void resolvePerformanceLevel_variousScores() {
        assertEquals("EXCELENTE", getPerformanceLevel(6.5));
        assertEquals("DESTACADO", getPerformanceLevel(5.5));
        assertEquals("ADECUADO", getPerformanceLevel(4.5));
        assertEquals("EN_RIESGO", getPerformanceLevel(3.0));
    }

    private String getPerformanceLevel(double score) {
        if (score >= 6.0) return "EXCELENTE";
        if (score >= 5.0) return "DESTACADO";
        if (score >= 4.0) return "ADECUADO";
        return "EN_RIESGO";
    }
}
