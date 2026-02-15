package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitorEvaluationAssignmentDTO;
import com.pdg.sigma.dto.MonitorEvaluationRequest;
import com.pdg.sigma.dto.MonitorEvaluationResponse;
import com.pdg.sigma.repository.MonitorEvaluationRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.service.MonitorEvaluationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorEvaluationServiceTest {

    @Mock
    private MonitorEvaluationRepository monitorEvaluationRepository;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @InjectMocks
    private MonitorEvaluationServiceImpl monitorEvaluationService;

    @Test
    void createEvaluation_successful() throws Exception {
        MonitorEvaluationRequest request = new MonitorEvaluationRequest();
        request.setProfessorId("PROF1");
        request.setMonitoringId(10L);
        request.setMonitorCode("MON001");
        request.setTaskCompliance(5);
        request.setTimelyCommunication(4);
        request.setPlanFulfillment(4);
        request.setAttitude(5);
        request.setComments("Excelente desempeño");
        request.setVisibleToMonitor(true);

        Professor professor = new Professor();
        professor.setId("PROF1");
        professor.setName("Profesor Uno");

        School school = new School();
        school.setName("Ingeniería");

        Program program = new Program();
        program.setName("Sistemas");
        program.setSchool(school);

        Course course = new Course();
        course.setId(4L);
        course.setName("Programación");
        course.setProgram(program);

        Monitoring monitoring = new Monitoring();
        monitoring.setId(10L);
        monitoring.setProfessor(professor);
        monitoring.setCourse(course);
        monitoring.setProgram(program);
        monitoring.setSemester("2025-1");

        Monitor monitor = new Monitor();
        monitor.setCode("MON001");
        monitor.setIdMonitor("12345");
        monitor.setName("Ana");
        monitor.setLastName("Pérez");
        monitor.setEmail("ana@example.com");

        MonitoringMonitor relation = new MonitoringMonitor();
        relation.setId(77L);
        relation.setMonitoring(monitoring);
        relation.setMonitor(monitor);
        relation.setEstadoSeleccion("aprobado");

        when(monitoringRepository.findById(10L)).thenReturn(Optional.of(monitoring));
        when(monitorRepository.findById("MON001")).thenReturn(Optional.of(monitor));
        when(monitorEvaluationRepository.findByMonitoringIdAndMonitorCode(10L, "MON001")).thenReturn(Optional.empty());
        when(professorRepository.findById("PROF1")).thenReturn(Optional.of(professor));
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(10L, "MON001")).thenReturn(Optional.of(relation));

        when(monitorEvaluationRepository.save(any(MonitorEvaluation.class))).thenAnswer(invocation -> {
            MonitorEvaluation eval = invocation.getArgument(0);
            eval.setId(99L);
            eval.setCreatedAt(LocalDateTime.now());
            eval.setUpdatedAt(eval.getCreatedAt());
            return eval;
        });

        MonitorEvaluationResponse response = monitorEvaluationService.createEvaluation(null, request);

        assertNotNull(response);
        assertEquals(99L, response.getEvaluationId());
        assertEquals("Programación - 2025-1", response.getMonitoringName());
        assertEquals("Ana Pérez", response.getMonitorFullName());
        assertEquals(4.5, response.getTotalScore());
        assertEquals("EXCELENTE", response.getPerformanceLevel());
        assertFalse(response.isPenaltyFlag());

        ArgumentCaptor<MonitorEvaluation> captor = ArgumentCaptor.forClass(MonitorEvaluation.class);
        verify(monitorEvaluationRepository, times(1)).save(captor.capture());
        MonitorEvaluation savedEvaluation = captor.getValue();
        assertEquals("Excelente desempeño", savedEvaluation.getQualitativeFeedback());
        assertEquals(5, savedEvaluation.getTaskCompliance());
    }

    @Test
    void getAssignments_filtersBySearch() throws Exception {
        String professorId = "PROF1";
        Professor professor = new Professor();
        professor.setId(professorId);
        professor.setName("Profesor Uno");

        School school = new School();
        school.setName("Ingeniería");

        Program program = new Program();
        program.setName("Sistemas");
        program.setSchool(school);

        Course course = new Course();
        course.setId(4L);
        course.setName("Programación");
        course.setProgram(program);

        Monitoring monitoring = new Monitoring();
        monitoring.setId(11L);
        monitoring.setProfessor(professor);
        monitoring.setCourse(course);
        monitoring.setProgram(program);
        monitoring.setSemester("2025-1");

        Monitor monitor = new Monitor();
        monitor.setCode("MON002");
        monitor.setIdMonitor("67890");
        monitor.setName("Carla");
        monitor.setLastName("Gómez");
        monitor.setEmail("carla@example.com");

        MonitoringMonitor relation = new MonitoringMonitor();
        relation.setId(78L);
        relation.setMonitoring(monitoring);
        relation.setMonitor(monitor);
        relation.setEstadoSeleccion("seleccionado");

        MonitorEvaluation evaluation = new MonitorEvaluation();
        evaluation.setId(101L);
        evaluation.setMonitoring(monitoring);
        evaluation.setMonitor(monitor);
        evaluation.setProfessor(professor);
        evaluation.setMonitoringMonitor(relation);
        evaluation.setVisibleToMonitor(true);
        evaluation.applyScores(4, 4, 3, 4, "Buen trabajo");
        evaluation.setCreatedAt(LocalDateTime.now());
        evaluation.setUpdatedAt(evaluation.getCreatedAt());

        when(professorRepository.findById(professorId)).thenReturn(Optional.of(professor));
        when(monitoringRepository.findByProfessor(professor)).thenReturn(List.of(monitoring));
        when(monitoringMonitorRepository.findByMonitoring(monitoring)).thenReturn(List.of(relation));
        when(monitorEvaluationRepository.findByMonitoringIds(List.of(11L))).thenReturn(List.of(evaluation));

        List<MonitorEvaluationAssignmentDTO> assignments = monitorEvaluationService
                .getEvaluationAssignmentsForProfessor(professorId, Optional.of("carla"));

        assertEquals(1, assignments.size());
        MonitorEvaluationAssignmentDTO dto = assignments.get(0);
        assertEquals("Carla Gómez", dto.getMonitorFullName());
        assertTrue(dto.isEvaluated());
        assertEquals(3.75, dto.getTotalScore());
        assertEquals("Programación - 2025-1", dto.getMonitoringName());
    }

    @Test
    void updateEvaluation_resetsAcknowledgementAndPersistsChanges() throws Exception {
        Long evaluationId = 120L;
        String professorId = "PROF-77";

        Professor professor = new Professor();
        professor.setId(professorId);

        Monitor monitor = new Monitor();
        monitor.setCode("MON-200");

        Monitoring monitoring = new Monitoring();
        monitoring.setId(55L);
        monitoring.setProfessor(professor);

        MonitorEvaluation existing = new MonitorEvaluation();
        existing.setId(evaluationId);
        existing.setProfessor(professor);
        existing.setMonitor(monitor);
        existing.setMonitoring(monitoring);
        existing.setVisibleToMonitor(true);
        existing.setAcknowledgedByMonitor(true);
        existing.setAcknowledgedAt(LocalDateTime.now().minusDays(1));
        existing.applyScores(5, 5, 5, 5, "Excelente");
        existing.setCreatedAt(LocalDateTime.now().minusDays(2));
        existing.setUpdatedAt(existing.getCreatedAt());

        MonitorEvaluationRequest request = new MonitorEvaluationRequest();
        request.setProfessorId(professorId);
        request.setTaskCompliance(2);
        request.setTimelyCommunication(3);
        request.setPlanFulfillment(2);
        request.setAttitude(3);
        request.setComments("Debe mejorar");
        request.setVisibleToMonitor(false);

        when(monitorEvaluationRepository.findById(evaluationId)).thenReturn(Optional.of(existing));
        when(monitorEvaluationRepository.save(any(MonitorEvaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MonitorEvaluationResponse response = monitorEvaluationService.updateEvaluation(evaluationId, professorId, request);

        assertNotNull(response);
        assertFalse(response.isVisibleToMonitor());
        assertEquals(2.5, response.getTotalScore());
        assertEquals("EN_RIESGO", response.getPerformanceLevel());
        assertFalse(response.isAcknowledgedByMonitor());
        verify(monitorEvaluationRepository).save(existing);
    }

    @Test
    void acknowledgeEvaluation_marksEvaluationWhenVisible() throws Exception {
        Long evaluationId = 300L;
        String monitorIdentifier = "MON-888";

        Monitor monitor = new Monitor();
        monitor.setCode(monitorIdentifier);
        monitor.setIdMonitor("ID-888");

        Professor professor = new Professor();
        professor.setId("PROF-1");

        Monitoring monitoring = new Monitoring();
        monitoring.setId(40L);
        monitoring.setProfessor(professor);

        MonitorEvaluation evaluation = new MonitorEvaluation();
        evaluation.setId(evaluationId);
        evaluation.setMonitor(monitor);
        evaluation.setMonitoring(monitoring);
        evaluation.setProfessor(professor);
        evaluation.setVisibleToMonitor(true);
        evaluation.setAcknowledgedByMonitor(false);
        evaluation.applyScores(4, 4, 4, 4, "Buen trabajo");
        evaluation.setCreatedAt(LocalDateTime.now().minusDays(1));
        evaluation.setUpdatedAt(evaluation.getCreatedAt());

        when(monitorRepository.findById(monitorIdentifier)).thenReturn(Optional.of(monitor));
        when(monitorEvaluationRepository.findById(evaluationId)).thenReturn(Optional.of(evaluation));
        when(monitorEvaluationRepository.save(any(MonitorEvaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MonitorEvaluationResponse response = monitorEvaluationService.acknowledgeEvaluation(evaluationId, monitorIdentifier);

        assertNotNull(response);
        assertTrue(response.isAcknowledgedByMonitor());
        assertNotNull(response.getAcknowledgedAt());
        verify(monitorEvaluationRepository).save(evaluation);
    }

    @Test
    void getEvaluationsForMonitor_filtersInvisibleRecords() throws Exception {
        String monitorIdentifier = "MON-500";

        Monitor monitor = new Monitor();
        monitor.setCode(monitorIdentifier);
        monitor.setIdMonitor("ID-500");

        when(monitorRepository.findById(monitorIdentifier)).thenReturn(Optional.of(monitor));

        MonitorEvaluation visible = new MonitorEvaluation();
        visible.setId(1L);
        visible.setMonitor(monitor);
        visible.setMonitoring(new Monitoring());
        visible.setProfessor(new Professor());
        visible.setVisibleToMonitor(true);
        visible.applyScores(4, 4, 4, 4, "Visible");
        visible.setCreatedAt(LocalDateTime.now().minusDays(1));
        visible.setUpdatedAt(visible.getCreatedAt());

        MonitorEvaluation hidden = new MonitorEvaluation();
        hidden.setId(2L);
        hidden.setMonitor(monitor);
        hidden.setMonitoring(new Monitoring());
        hidden.setProfessor(new Professor());
        hidden.setVisibleToMonitor(false);
        hidden.applyScores(5, 5, 5, 5, "Oculto");
        hidden.setCreatedAt(LocalDateTime.now().minusDays(1));
        hidden.setUpdatedAt(hidden.getCreatedAt());

        when(monitorEvaluationRepository.findByMonitorCodeOrderByCreatedAtDesc(monitorIdentifier))
                .thenReturn(List.of(visible, hidden));

        List<MonitorEvaluationResponse> responses = monitorEvaluationService.getEvaluationsForMonitor(monitorIdentifier);

        assertEquals(1, responses.size());
        assertEquals(visible.getId(), responses.get(0).getEvaluationId());
        assertEquals("Visible", responses.get(0).getComments());
        assertTrue(responses.get(0).isVisibleToMonitor());
    }
}
