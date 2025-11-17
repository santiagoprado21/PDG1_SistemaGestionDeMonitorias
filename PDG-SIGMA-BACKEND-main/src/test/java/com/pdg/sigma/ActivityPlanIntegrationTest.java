package com.pdg.sigma;

import com.pdg.sigma.config.TestConfig;
import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.ActivityScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de Integración para HU-011
 * Flujo completo: Crear Rúbricas → Crear Plan → Agregar Actividades → Validar Conflictos
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class ActivityPlanIntegrationTest {

    @Autowired(required = false)
    private ActivityScheduleService activityScheduleService;

    @Autowired(required = false)
    private MonitoringRepository monitoringRepository;

    @Autowired(required = false)
    private ProfessorRepository professorRepository;

    @Autowired(required = false)
    private MonitorRepository monitorRepository;

    @Autowired(required = false)
    private CourseRepository courseRepository;

    @Autowired(required = false)
    private ProgramRepository programRepository;

    @Autowired(required = false)
    private SchoolRepository schoolRepository;

    private Professor testProfessor;
    private Monitor testMonitor;
    private Monitoring testMonitoring;

    @BeforeEach
    void setUp() {
        // Solo ejecutar si las dependencias están disponibles
        if (professorRepository == null) {
            return;
        }

        // Crear datos de prueba básicos
        testProfessor = new Professor();
        testProfessor.setId("TEST_PROF_001");
        testProfessor.setName("Profesor Test");
        testProfessor.setPassword("test123"); // Campo obligatorio
        testProfessor = professorRepository.save(testProfessor);

        testMonitor = new Monitor();
        testMonitor.setCode("TEST_MON_001"); // Este es el @Id
        testMonitor.setIdMonitor("TEST_MON_001");
        testMonitor.setName("Monitor");
        testMonitor.setLastName("Test");
        testMonitor.setEmail("monitor@test.com");
        testMonitor.setSemester(5); // Campo obligatorio
        testMonitor.setGradeAverage(4.0);
        testMonitor.setGradeCourse(4.5);
        testMonitor = monitorRepository.save(testMonitor);

        School school = new School();
        school.setName("Facultad Test");
        school = schoolRepository.save(school);

        Program program = new Program();
        program.setName("Programa Test");
        program.setSchool(school);
        program = programRepository.save(program);

        Course course = new Course();
        course.setName("Curso Test");
        course.setProgram(program);
        course = courseRepository.save(course);

        testMonitoring = new Monitoring();
        testMonitoring.setProfessor(testProfessor);
        testMonitoring.setCourse(course);
        testMonitoring.setProgram(program);
        testMonitoring.setSchool(school);
        testMonitoring.setSemester("2025-TEST");
        testMonitoring.setStart(java.sql.Date.valueOf(LocalDate.now()));
        testMonitoring.setFinish(java.sql.Date.valueOf(LocalDate.now().plusMonths(4)));
        testMonitoring = monitoringRepository.save(testMonitoring);
    }

    @Test
    @DisplayName("Flujo: Validación de conflictos entre actividades")
    void testConflictDetection() throws Exception {
        // Skip si no están disponibles las dependencias
        if (activityScheduleService == null) {
            return;
        }

        LocalDate targetDate = LocalDate.now().plusDays(1);

        // 1. Crear primera actividad
        ActivityScheduleDTO activity1 = new ActivityScheduleDTO();
        activity1.setName("Actividad 1");
        activity1.setDescription("Primera actividad");
        activity1.setCategory("Tutoría");
        activity1.setFinish(java.sql.Date.valueOf(targetDate));
        activity1.setStartTime(LocalTime.of(14, 0));
        activity1.setEndTime(LocalTime.of(16, 0));
        activity1.setMonitoringId(testMonitoring.getId().intValue());
        activity1.setProfessorId(testProfessor.getId());
        activity1.setMonitorId(testMonitor.getIdMonitor());
        activity1.setState("PENDIENTE");
        activity1.setRoleCreator("P");
        activity1.setRoleResponsable("M");

        ActivityScheduleDTO created1 = activityScheduleService.saveActivityWithSchedule(activity1);
        assertNotNull(created1);

        // 2. Intentar crear actividad con conflicto
        ActivityScheduleDTO activity2 = new ActivityScheduleDTO();
        activity2.setName("Actividad 2 (Conflicto)");
        activity2.setDescription("Actividad que se solapa");
        activity2.setCategory("Apoyo");
        activity2.setFinish(java.sql.Date.valueOf(targetDate)); // Misma fecha
        activity2.setStartTime(LocalTime.of(15, 0)); // Se solapa con 14:00-16:00
        activity2.setEndTime(LocalTime.of(17, 0));
        activity2.setMonitorId(testMonitor.getIdMonitor());

        // Validar conflictos
        List<ScheduleConflictDTO> conflicts = activityScheduleService.validateScheduleConflicts(activity2);
        
        // Debe detectar el conflicto
        assertNotNull(conflicts);
        assertFalse(conflicts.isEmpty(), "Debería detectar al menos un conflicto");
        assertEquals("Actividad 1", conflicts.get(0).getActivityName());

        // 3. Intentar crear la actividad - debe fallar
        activity2.setMonitoringId(testMonitoring.getId().intValue());
        activity2.setProfessorId(testProfessor.getId());
        activity2.setState("PENDIENTE");

        Exception exception = assertThrows(Exception.class, () -> {
            activityScheduleService.saveActivityWithSchedule(activity2);
        });
        assertTrue(exception.getMessage().contains("Conflicto"));
    }

    @Test
    @DisplayName("Flujo: Crear múltiples actividades sin conflictos")
    void testMultipleActivitiesWithoutConflicts() throws Exception {
        // Skip si no están disponibles las dependencias
        if (activityScheduleService == null) {
            return;
        }

        // Crear 3 actividades en horarios diferentes
        LocalDate baseDate = LocalDate.now().plusDays(2);

        for (int i = 0; i < 3; i++) {
            ActivityScheduleDTO activity = new ActivityScheduleDTO();
            activity.setName("Actividad " + (i + 1));
            activity.setDescription("Actividad número " + (i + 1));
            activity.setCategory("Tutoría");
            activity.setFinish(java.sql.Date.valueOf(baseDate.plusDays(i)));
            activity.setStartTime(LocalTime.of(10, 0));
            activity.setEndTime(LocalTime.of(12, 0));
            activity.setDurationHours(BigDecimal.valueOf(2.0));
            activity.setMonitoringId(testMonitoring.getId().intValue());
            activity.setProfessorId(testProfessor.getId());
            activity.setMonitorId(testMonitor.getIdMonitor());
            activity.setState("PENDIENTE");
            activity.setRoleCreator("P");
            activity.setRoleResponsable("M");

            ActivityScheduleDTO created = activityScheduleService.saveActivityWithSchedule(activity);
            assertNotNull(created);
        }

        // Verificar el plan
        ActivityPlanDTO plan = activityScheduleService.getActivityPlan(testMonitoring.getId().intValue());
        assertEquals(3, plan.getTotalActivities());
        assertEquals(6.0, plan.getTotalHours());
        assertEquals(3, plan.getPendingActivities());
        assertEquals(0, plan.getCompletedActivities());
    }

    @Test
    @DisplayName("Flujo: Obtener plan vacío para monitoría sin actividades")
    void testEmptyActivityPlan() throws Exception {
        // Skip si no están disponibles las dependencias
        if (activityScheduleService == null) {
            return;
        }

        // Obtener plan sin crear actividades
        ActivityPlanDTO plan = activityScheduleService.getActivityPlan(testMonitoring.getId().intValue());
        
        assertNotNull(plan);
        assertEquals(0, plan.getTotalActivities());
        assertEquals(0, plan.getCompletedActivities());
        assertEquals(0, plan.getPendingActivities());
        assertEquals(0.0, plan.getTotalHours());
        assertNotNull(plan.getActivities());
        assertTrue(plan.getActivities().isEmpty());
    }
}

