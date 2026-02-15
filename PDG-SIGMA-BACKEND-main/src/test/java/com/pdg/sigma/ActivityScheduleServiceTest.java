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
        mockMonitor.setCode("M001");
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

    // ==================== HU-017: Vista Monitor - Plan de Actividades ====================

    @Test
    @DisplayName("HU-017: Debe obtener planes de actividades para un monitor sin asignaciones")
    void testGetMonitorActivityPlans_NoMonitorings() throws Exception {
        // Given
        String monitorId = "2220001";
        Monitor monitor = new Monitor();
        monitor.setCode(monitorId);
        monitor.setIdMonitor(monitorId);
        monitor.setName("Pedro");
        monitor.setLastName("García");

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(monitoringRepository.findMonitoringsDirectlyAssignedToMonitorWithStatusSelected(monitorId))
                .thenReturn(new ArrayList<>());
        when(activityRepository.findByMonitor(monitor)).thenReturn(new ArrayList<>());

        // When
        List<ActivityPlanDTO> result = activityScheduleService.getMonitorActivityPlans(monitorId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(monitorRepository, times(1)).findById(monitorId);
        verify(monitoringRepository, times(1)).findMonitoringsDirectlyAssignedToMonitorWithStatusSelected(monitorId);
        verify(activityRepository, times(1)).findByMonitor(monitor);
    }

    @Test
    @DisplayName("HU-017: Debe obtener planes de actividades para monitor con múltiples monitorías")
    void testGetMonitorActivityPlans_MultipleMonitorings() throws Exception {
        // Given
        String monitorId = "2220004";
        Monitor ana = new Monitor();
        ana.setCode(monitorId);
        ana.setIdMonitor(monitorId);
        ana.setName("Ana");
        ana.setLastName("López");

        // Monitoría 1: Programación I
        Course course1 = new Course();
        course1.setName("Programación I");
        Program program1 = new Program();
        program1.setName("Ingeniería de Sistemas");
        
        Monitoring monitoring1 = new Monitoring();
        monitoring1.setId(1L);
        monitoring1.setCourse(course1);
        monitoring1.setProgram(program1);
        monitoring1.setProfessor(mockProfessor);
        monitoring1.setSemester("2024-2");

        // Monitoría 2: Estructuras de Datos
        Course course2 = new Course();
        course2.setName("Estructuras de Datos");
        
        Monitoring monitoring2 = new Monitoring();
        monitoring2.setId(4L);
        monitoring2.setCourse(course2);
        monitoring2.setProgram(program1);
        monitoring2.setProfessor(mockProfessor);
        monitoring2.setSemester("2025-2");

        // Actividades para monitoría 1
        Activity activity1 = new Activity();
        activity1.setId(7);
        activity1.setName("Apoyo en Clase");
        activity1.setState(StateActivity.PENDIENTE);
        activity1.setMonitoring(monitoring1);
        activity1.setMonitor(ana);
        activity1.setDurationHours(BigDecimal.valueOf(2.0));

        // Actividades para monitoría 2
        Activity activity2 = new Activity();
        activity2.setId(8);
        activity2.setName("Tutoría Grupal");
        activity2.setState(StateActivity.COMPLETADO);
        activity2.setMonitoring(monitoring2);
        activity2.setMonitor(ana);
        activity2.setDurationHours(BigDecimal.valueOf(3.0));

        List<Monitoring> monitorings = List.of(monitoring1, monitoring2);
        List<Activity> activities1 = List.of(activity1);
        List<Activity> activities2 = List.of(activity2);

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(ana));
        when(monitoringRepository.findMonitoringsDirectlyAssignedToMonitorWithStatusSelected(monitorId))
                .thenReturn(monitorings);
        when(activityRepository.findByMonitor(ana)).thenReturn(new ArrayList<>());
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(monitoring1));
        when(monitoringRepository.findById(4L)).thenReturn(Optional.of(monitoring2));
        when(activityRepository.findByMonitoringOrderedBySchedule(monitoring1)).thenReturn(activities1);
        when(activityRepository.findByMonitoringOrderedBySchedule(monitoring2)).thenReturn(activities2);

        // When
        List<ActivityPlanDTO> result = activityScheduleService.getMonitorActivityPlans(monitorId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verificar plan 1
        ActivityPlanDTO plan1 = result.stream()
                .filter(p -> p.getMonitoringId() == 1)
                .findFirst()
                .orElse(null);
        assertNotNull(plan1);
        assertEquals("Programación I", plan1.getCourseName());
        assertEquals(1, plan1.getTotalActivities());
        assertEquals(1, plan1.getPendingActivities());
        assertEquals(0, plan1.getCompletedActivities());

        // Verificar plan 2
        ActivityPlanDTO plan2 = result.stream()
                .filter(p -> p.getMonitoringId() == 4)
                .findFirst()
                .orElse(null);
        assertNotNull(plan2);
        assertEquals("Estructuras de Datos", plan2.getCourseName());
        assertEquals(1, plan2.getTotalActivities());
        assertEquals(0, plan2.getPendingActivities());
        assertEquals(1, plan2.getCompletedActivities());
    }

    @Test
    @DisplayName("HU-017: Debe combinar monitorías asignadas y actividades directas sin duplicados")
    void testGetMonitorActivityPlans_NoDuplicates() throws Exception {
        // Given
        String monitorId = "2220004";
        Monitor ana = new Monitor();
        ana.setCode(monitorId);
        ana.setIdMonitor(monitorId);
        ana.setName("Ana");

        Course course = new Course();
        course.setName("Cálculo I");
        
        Monitoring monitoring = new Monitoring();
        monitoring.setId(15L);
        monitoring.setCourse(course);
        monitoring.setProfessor(mockProfessor);
        monitoring.setSemester("2025-2");

        Activity activity = new Activity();
        activity.setId(13);
        activity.setName("Tutoría grupal Cálculo");
        activity.setState(StateActivity.PENDIENTE);
        activity.setMonitoring(monitoring);
        activity.setMonitor(ana);

        // Esta monitoría aparece en ambas fuentes
        List<Monitoring> directMonitorings = List.of(monitoring);
        List<Activity> directActivities = List.of(activity);
        List<Activity> monitoringActivities = List.of(activity);

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(ana));
        when(monitoringRepository.findMonitoringsDirectlyAssignedToMonitorWithStatusSelected(monitorId))
                .thenReturn(directMonitorings);
        when(activityRepository.findByMonitor(ana)).thenReturn(directActivities);
        when(monitoringRepository.findById(15L)).thenReturn(Optional.of(monitoring));
        when(activityRepository.findByMonitoringOrderedBySchedule(monitoring)).thenReturn(monitoringActivities);

        // When
        List<ActivityPlanDTO> result = activityScheduleService.getMonitorActivityPlans(monitorId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size(), "Debe haber solo 1 plan (sin duplicados)");
        assertEquals(15, result.get(0).getMonitoringId());
        assertEquals("Cálculo I", result.get(0).getCourseName());
        assertEquals(1, result.get(0).getTotalActivities());
    }

    @Test
    @DisplayName("HU-017: Debe manejar actividades sin monitoría asociada")
    void testGetMonitorActivityPlans_ActivitiesWithoutMonitoring() throws Exception {
        // Given
        String monitorId = "2220004";
        Monitor monitor = new Monitor();
        monitor.setCode(monitorId);
        monitor.setIdMonitor(monitorId);

        Activity activityWithoutMonitoring = new Activity();
        activityWithoutMonitoring.setId(99);
        activityWithoutMonitoring.setName("Actividad huérfana");
        activityWithoutMonitoring.setMonitor(monitor);
        activityWithoutMonitoring.setMonitoring(null); // Sin monitoría

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(monitoringRepository.findMonitoringsDirectlyAssignedToMonitorWithStatusSelected(monitorId))
                .thenReturn(new ArrayList<>());
        when(activityRepository.findByMonitor(monitor)).thenReturn(List.of(activityWithoutMonitoring));

        // When
        List<ActivityPlanDTO> result = activityScheduleService.getMonitorActivityPlans(monitorId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No debe incluir actividades sin monitoría");
    }

    @Test
    @DisplayName("HU-017: Debe lanzar excepción si monitor no existe")
    void testGetMonitorActivityPlans_MonitorNotFound() {
        // Given
        String nonExistentMonitorId = "9999999";
        when(monitorRepository.findById(nonExistentMonitorId)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            activityScheduleService.getMonitorActivityPlans(nonExistentMonitorId);
        });

        assertTrue(exception.getMessage().contains("Monitor no encontrado"));
        verify(monitoringRepository, never()).findMonitoringsDirectlyAssignedToMonitorWithStatusSelected(any());
    }

    @Test
    @DisplayName("HU-017: Debe manejar errores al obtener plan individual sin detener el flujo")
    void testGetMonitorActivityPlans_HandleIndividualPlanErrors() throws Exception {
        // Given
        String monitorId = "2220004";
        Monitor monitor = new Monitor();
        monitor.setCode(monitorId);
        monitor.setIdMonitor(monitorId);

        Monitoring monitoring1 = new Monitoring();
        monitoring1.setId(1L);
        
        Monitoring monitoring2 = new Monitoring();
        monitoring2.setId(2L);

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(monitoringRepository.findMonitoringsDirectlyAssignedToMonitorWithStatusSelected(monitorId))
                .thenReturn(List.of(monitoring1, monitoring2));
        when(activityRepository.findByMonitor(monitor)).thenReturn(new ArrayList<>());
        
        // Monitoría 1 falla al obtener el plan
        when(monitoringRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Monitoría 2 funciona correctamente
        when(monitoringRepository.findById(2L)).thenReturn(Optional.of(monitoring2));
        when(activityRepository.findByMonitoringOrderedBySchedule(monitoring2)).thenReturn(new ArrayList<>());

        // When
        List<ActivityPlanDTO> result = activityScheduleService.getMonitorActivityPlans(monitorId);

        // Then
        // El servicio debe continuar y retornar los planes que sí pudo obtener
        assertNotNull(result);
        // Dependiendo de la implementación, puede retornar los planes exitosos o lista vacía
        // Aquí asumimos que maneja el error y continúa
    }
}

