package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitoringClosureRequest;
import com.pdg.sigma.dto.MonitoringClosureReport;
import com.pdg.sigma.repository.ActivityRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HU-007: Tests para MonitoringClosureService
 */
@ExtendWith(MockitoExtension.class)
class MonitoringClosureServiceTest {

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private MonitoringClosureServiceImpl monitoringClosureService;

    private Monitoring testMonitoring;
    private MonitoringClosureRequest testRequest;

    @BeforeEach
    void setUp() {
        // Crear monitoría de prueba
        testMonitoring = new Monitoring();
        testMonitoring.setId(1L);
        testMonitoring.setSemester("2026-1");
        testMonitoring.setApprovalStatus(MonitoringApprovalStatus.APROBADA);
        testMonitoring.setEstimatedHours(40);
        testMonitoring.setHourlyRate(15000.0);

        // Crear curso, programa, profesor y monitor mock
        Course course = new Course();
        course.setName("Programación Avanzada");
        testMonitoring.setCourse(course);

        Program program = new Program();
        program.setName("Ingeniería de Sistemas");
        testMonitoring.setProgram(program);

        Professor professor = new Professor();
        professor.setName("Juan Pérez");
        testMonitoring.setProfessor(professor);

        Monitor monitor = new Monitor();
        monitor.setName("Ana García");
        testMonitoring.setAssignedMonitor(monitor);

        // Request de prueba
        testRequest = new MonitoringClosureRequest();
        testRequest.setComment("Cierre de prueba");
        testRequest.setAutoCalculate(true);
    }

    @Test
    void testGetMonitoringsReadyForClosure_ReturnsApprovedMonitorings() {
        // Arrange
        List<Monitoring> expectedMonitorings = Arrays.asList(testMonitoring);
        when(monitoringRepository.findBySemesterAndApprovalStatus("2026-1", MonitoringApprovalStatus.APROBADA))
                .thenReturn(expectedMonitorings);

        // Act
        List<Monitoring> result = monitoringClosureService.getMonitoringsReadyForClosure("2026-1", null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMonitoring.getId(), result.get(0).getId());
        verify(monitoringRepository).findBySemesterAndApprovalStatus("2026-1", MonitoringApprovalStatus.APROBADA);
    }

    @Test
    void testCloseMonitoring_WithAutoCalculate_Success() throws Exception {
        // Arrange
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(testMonitoring));
        when(activityRepository.findByMonitoringId(1L)).thenReturn(Arrays.asList(
                createActivity(StateActivity.COMPLETADO),
                createActivity(StateActivity.COMPLETADO),
                createActivity(StateActivity.PENDIENTE)
        ));
        when(monitoringRepository.save(any(Monitoring.class))).thenReturn(testMonitoring);

        // Act
        MonitoringClosureReport report = monitoringClosureService.closeMonitoring(1L, testRequest, "5001");

        // Assert
        assertNotNull(report);
        assertEquals(1L, report.getMonitoringId());
        assertEquals("5001", report.getClosedBy());
        assertEquals("Cierre de prueba", report.getClosureComment());
        assertTrue(report.getCompliancePercentage() >= 0);
        verify(monitoringRepository).save(any(Monitoring.class));
    }

    @Test
    void testCloseMonitoring_AlreadyClosed_ThrowsException() {
        // Arrange
        testMonitoring.setApprovalStatus(MonitoringApprovalStatus.CERRADA);
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(testMonitoring));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            monitoringClosureService.closeMonitoring(1L, testRequest, "5001");
        });

        assertTrue(exception.getMessage().contains("no puede ser cerrada"));
    }

    @Test
    void testCloseMonitoring_NotFound_ThrowsException() {
        // Arrange
        when(monitoringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            monitoringClosureService.closeMonitoring(999L, testRequest, "5001");
        });

        assertTrue(exception.getMessage().contains("no encontrada"));
    }

    @Test
    void testCloseMonitoringsBatch_Success() throws Exception {
        // Arrange
        Monitoring monitoring2 = new Monitoring();
        monitoring2.setId(2L);
        monitoring2.setSemester("2026-1");
        monitoring2.setApprovalStatus(MonitoringApprovalStatus.APROBADA);
        monitoring2.setCourse(testMonitoring.getCourse());
        monitoring2.setProgram(testMonitoring.getProgram());
        monitoring2.setProfessor(testMonitoring.getProfessor());

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(testMonitoring));
        when(monitoringRepository.findById(2L)).thenReturn(Optional.of(monitoring2));
        when(activityRepository.findByMonitoringId(anyLong())).thenReturn(Arrays.asList(
                createActivity(StateActivity.COMPLETADO)
        ));
        when(monitoringRepository.save(any(Monitoring.class))).thenReturn(testMonitoring);

        // Act
        List<MonitoringClosureReport> reports = monitoringClosureService.closeMonitoringsBatch(
                Arrays.asList(1L, 2L), "5001", testRequest
        );

        // Assert
        assertNotNull(reports);
        assertEquals(2, reports.size());
        verify(monitoringRepository, times(2)).save(any(Monitoring.class));
    }

    @Test
    void testGetClosedMonitorings_ReturnsClosedOnly() {
        // Arrange
        testMonitoring.setApprovalStatus(MonitoringApprovalStatus.CERRADA);
        List<Monitoring> closedMonitorings = Arrays.asList(testMonitoring);
        when(monitoringRepository.findBySemesterAndApprovalStatus("2026-1", MonitoringApprovalStatus.CERRADA))
                .thenReturn(closedMonitorings);

        // Act
        List<Monitoring> result = monitoringClosureService.getClosedMonitorings("2026-1", null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(MonitoringApprovalStatus.CERRADA, result.get(0).getApprovalStatus());
    }

    @Test
    void testGenerateComplianceReport_Success() throws Exception {
        // Arrange
        testMonitoring.setApprovalStatus(MonitoringApprovalStatus.CERRADA);
        testMonitoring.setClosedBy("5001");
        testMonitoring.setClosureComment("Test cierre");
        testMonitoring.setCompliancePercentage(85);
        testMonitoring.setCompletedActivities(8);
        testMonitoring.setTotalActivities(10);
        
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(testMonitoring));

        // Act
        MonitoringClosureReport report = monitoringClosureService.generateComplianceReport(1L);

        // Assert
        assertNotNull(report);
        assertEquals(1L, report.getMonitoringId());
        assertEquals("5001", report.getClosedBy());
        assertEquals("Test cierre", report.getClosureComment());
        assertEquals(85, report.getCompliancePercentage());
        assertEquals(8, report.getCompletedActivities());
        assertEquals(10, report.getTotalActivities());
    }

    // Helper method
    private Activity createActivity(StateActivity state) {
        Activity activity = new Activity();
        activity.setState(state);
        activity.setDurationHours(java.math.BigDecimal.valueOf(2.0));
        return activity;
    }
}
