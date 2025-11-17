package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.ActivityPlanDTO;
import com.pdg.sigma.dto.ActivityScheduleDTO;
import com.pdg.sigma.dto.ScheduleConflictDTO;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.ActivityScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas Unitarias para ActivityScheduleService
 * HU-011: Creación de plan de actividades para monitores
 */
class ActivityScheduleServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private RubricRepository rubricRepository;

    @InjectMocks
    private ActivityScheduleServiceImpl activityScheduleService;

    private Monitoring mockMonitoring;
    private Monitor mockMonitor;
    private Professor mockProfessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock data
        mockProfessor = new Professor();
        mockProfessor.setId("1001");
        mockProfessor.setName("Dr. Test");

        mockMonitor = new Monitor();
        mockMonitor.setIdMonitor("M001");
        mockMonitor.setName("Juan");
        mockMonitor.setLastName("Pérez");

        Course course = new Course();
        course.setName("Programación I");

        Program program = new Program();
        program.setName("Ingeniería de Sistemas");

        mockMonitoring = new Monitoring();
        mockMonitoring.setId(1L);
        mockMonitoring.setCourse(course);
        mockMonitoring.setProgram(program);
        mockMonitoring.setProfessor(mockProfessor);
        mockMonitoring.setSemester("2025-1");
    }

    @Test
    @DisplayName("Debe crear actividad sin horario específico")
    void testCreateActivityWithoutSchedule() throws Exception {
        // Given
        ActivityScheduleDTO dto = new ActivityScheduleDTO();
        dto.setName("Preparar material");
        dto.setDescription("Crear presentaciones");
        dto.setCategory("Preparación de material");
        dto.setFinish(java.sql.Date.valueOf(LocalDate.now().plusDays(7)));
        dto.setMonitoringId(1);
        dto.setProfessorId("1001");
        dto.setMonitorId("M001");
        dto.setState("PENDIENTE");

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(professorRepository.findById("1001")).thenReturn(Optional.of(mockProfessor));
        when(monitorRepository.findById("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.save(any(Activity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ActivityScheduleDTO result = activityScheduleService.saveActivityWithSchedule(dto);

        // Then
        assertNotNull(result);
        assertEquals("Preparar material", result.getName());
        verify(activityRepository, times(1)).save(any(Activity.class));
    }

    @Test
    @DisplayName("Debe crear actividad con horario específico")
    void testCreateActivityWithSchedule() throws Exception {
        // Given
        ActivityScheduleDTO dto = new ActivityScheduleDTO();
        dto.setName("Tutoría");
        dto.setDescription("Apoyo a estudiantes");
        dto.setCategory("Tutoría");
        dto.setFinish(java.sql.Date.valueOf(LocalDate.now().plusDays(1)));
        dto.setStartTime(LocalTime.of(14, 0));
        dto.setEndTime(LocalTime.of(16, 0));
        dto.setDurationHours(BigDecimal.valueOf(2.0));
        dto.setMonitoringId(1);
        dto.setProfessorId("1001");
        dto.setMonitorId("M001");
        dto.setState("PENDIENTE");

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(professorRepository.findById("1001")).thenReturn(Optional.of(mockProfessor));
        when(monitorRepository.findById("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.findScheduleConflicts(any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(activityRepository.save(any(Activity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ActivityScheduleDTO result = activityScheduleService.saveActivityWithSchedule(dto);

        // Then
        assertNotNull(result);
        assertEquals("Tutoría", result.getName());
        assertEquals(LocalTime.of(14, 0), result.getStartTime());
        assertEquals(LocalTime.of(16, 0), result.getEndTime());
        verify(activityRepository, times(1)).findScheduleConflicts(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe detectar conflictos de horarios")
    void testDetectScheduleConflicts() throws Exception {
        // Given
        ActivityScheduleDTO dto = new ActivityScheduleDTO();
        dto.setFinish(java.sql.Date.valueOf(LocalDate.now().plusDays(1)));
        dto.setStartTime(LocalTime.of(14, 0));
        dto.setEndTime(LocalTime.of(16, 0));
        dto.setMonitorId("M001");

        Activity conflictingActivity = new Activity();
        conflictingActivity.setId(1);
        conflictingActivity.setName("Actividad Existente");
        conflictingActivity.setCategory("Tutoría");
        conflictingActivity.setFinish(dto.getFinish());
        conflictingActivity.setStartTime(LocalTime.of(15, 0));
        conflictingActivity.setEndTime(LocalTime.of(17, 0));

        List<Activity> conflicts = new ArrayList<>();
        conflicts.add(conflictingActivity);

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.findScheduleConflicts(any(), any(), any(), any(), any()))
                .thenReturn(conflicts);

        // When
        List<ScheduleConflictDTO> result = activityScheduleService.validateScheduleConflicts(dto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Actividad Existente", result.get(0).getActivityName());
    }

    @Test
    @DisplayName("No debe permitir hora fin menor que hora inicio")
    void testInvalidTimeRange() {
        // Given
        ActivityScheduleDTO dto = new ActivityScheduleDTO();
        dto.setFinish(java.sql.Date.valueOf(LocalDate.now().plusDays(1)));
        dto.setStartTime(LocalTime.of(16, 0));
        dto.setEndTime(LocalTime.of(14, 0)); // Fin antes de inicio
        dto.setMonitorId("M001");

        when(monitorRepository.findById("M001")).thenReturn(Optional.of(mockMonitor));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            activityScheduleService.validateScheduleConflicts(dto);
        });

        assertTrue(exception.getMessage().contains("hora de inicio debe ser anterior"));
    }

    @Test
    @DisplayName("Debe obtener plan de actividades de una monitoría")
    void testGetActivityPlan() throws Exception {
        // Given
        List<Activity> activities = new ArrayList<>();
        
        Activity activity1 = new Activity();
        activity1.setId(1);
        activity1.setName("Tutoría");
        activity1.setState(StateActivity.PENDIENTE);
        activity1.setDurationHours(BigDecimal.valueOf(2.0));
        
        Activity activity2 = new Activity();
        activity2.setId(2);
        activity2.setName("Preparar material");
        activity2.setState(StateActivity.COMPLETADO);
        activity2.setDurationHours(BigDecimal.valueOf(1.5));
        
        activities.add(activity1);
        activities.add(activity2);

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(activityRepository.findByMonitoringOrderedBySchedule(mockMonitoring))
                .thenReturn(activities);

        // When
        ActivityPlanDTO result = activityScheduleService.getActivityPlan(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getMonitoringId());
        assertEquals("Programación I", result.getCourseName());
        assertEquals("Ingeniería de Sistemas", result.getProgramName());
        assertEquals(2, result.getTotalActivities());
        assertEquals(1, result.getCompletedActivities());
        assertEquals(1, result.getPendingActivities());
        assertEquals(3.5, result.getTotalHours());
    }

    @Test
    @DisplayName("Debe rechazar actividad con conflicto de horarios")
    void testRejectActivityWithConflict() throws Exception {
        // Given
        ActivityScheduleDTO dto = new ActivityScheduleDTO();
        dto.setName("Nueva Tutoría");
        dto.setFinish(java.sql.Date.valueOf(LocalDate.now().plusDays(1)));
        dto.setStartTime(LocalTime.of(14, 0));
        dto.setEndTime(LocalTime.of(16, 0));
        dto.setMonitoringId(1);
        dto.setProfessorId("1001");
        dto.setMonitorId("M001");

        Activity conflictingActivity = new Activity();
        conflictingActivity.setName("Tutoría Existente");
        List<Activity> conflicts = List.of(conflictingActivity);

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(professorRepository.findById("1001")).thenReturn(Optional.of(mockProfessor));
        when(monitorRepository.findById("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.findScheduleConflicts(any(), any(), any(), any(), any()))
                .thenReturn(conflicts);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            activityScheduleService.saveActivityWithSchedule(dto);
        });

        assertTrue(exception.getMessage().contains("Conflicto de horarios"));
        verify(activityRepository, never()).save(any());
    }
}

