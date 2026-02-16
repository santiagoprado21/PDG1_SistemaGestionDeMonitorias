package com.pdg.sigma;

import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.SupervisorEvaluation;
import com.pdg.sigma.dto.SupervisorEvaluationRequest;
import com.pdg.sigma.dto.SupervisorEvaluationResponse;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.SupervisorEvaluationRepository;
import com.pdg.sigma.service.SupervisorEvaluationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupervisorEvaluationServiceTest {

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

    @InjectMocks
    private SupervisorEvaluationServiceImpl supervisorEvaluationService;

    @Test
    void createEvaluation_successful() throws Exception {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(25L);
        request.setMonitorIdentifier("MON-10");
        request.setGuidanceClarity(6);
        request.setRoleExpectations(6);
        request.setAvailabilityDisposition(6);
        request.setSupportTimeliness(6);
        request.setFeedbackConstructive(6);
        request.setFeedbackFairness(6);
        request.setRespectfulTreatment(6);
        request.setTrustEnvironment(6);
        request.setStrengthsComments("Clarity and support");
        request.setImprovementComments("More check-ins");

        Professor professor = new Professor();
        professor.setId("PROF-1");
        professor.setName("Professor One");

        Monitoring monitoring = new Monitoring();
        monitoring.setId(25L);
        monitoring.setProfessor(professor);
        monitoring.setSemester("2025-2");

        Monitor monitor = new Monitor();
        monitor.setCode("MON-10");
        monitor.setIdMonitor("10010");
        monitor.setName("Ana");
        monitor.setLastName("Diaz");
        monitor.setEmail("ana@example.com");

        monitoring.setAssignedMonitor(monitor);

        when(monitoringRepository.findById(25L)).thenReturn(Optional.of(monitoring));
        when(monitorRepository.findById("MON-10")).thenReturn(Optional.of(monitor));
        when(supervisorEvaluationRepository.findByMonitoringIdAndMonitorCode(25L, "MON-10")).thenReturn(Optional.empty());
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(25L, "MON-10")).thenReturn(Optional.empty());

        when(supervisorEvaluationRepository.save(any(SupervisorEvaluation.class))).thenAnswer(invocation -> {
            SupervisorEvaluation evaluation = invocation.getArgument(0);
            evaluation.setId(101L);
            evaluation.setCreatedAt(LocalDateTime.now());
            evaluation.setUpdatedAt(evaluation.getCreatedAt());
            return evaluation;
        });

        SupervisorEvaluationResponse response = supervisorEvaluationService.createEvaluation(null, request);

        assertNotNull(response);
        assertEquals(101L, response.getEvaluationId());
        assertEquals(6.0, response.getTotalScore());
        assertEquals("EXCELENTE", response.getPerformanceLevel());
        assertEquals("Clarity and support", response.getStrengthsComments());

        ArgumentCaptor<SupervisorEvaluation> captor = ArgumentCaptor.forClass(SupervisorEvaluation.class);
        verify(supervisorEvaluationRepository, times(1)).save(captor.capture());
        SupervisorEvaluation saved = captor.getValue();
        assertEquals(6, saved.getGuidanceClarity());
        assertEquals(6, saved.getTrustEnvironment());
    }

    @Test
    void createEvaluation_rejectsScoreOutOfRange() {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(25L);
        request.setMonitorIdentifier("MON-10");
        request.setGuidanceClarity(8);

        Exception error = assertThrows(Exception.class, () -> supervisorEvaluationService.createEvaluation("MON-10", request));
        assertNotNull(error.getMessage());
        assertEquals(true, error.getMessage().contains("Claridad de la"));
        assertEquals(true, error.getMessage().contains("1 y 7"));
    }
}
