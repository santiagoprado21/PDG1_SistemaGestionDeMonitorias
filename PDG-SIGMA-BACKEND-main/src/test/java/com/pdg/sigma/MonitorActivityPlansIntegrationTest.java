package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de Integración para HU-017
 * Vista Monitor - Plan de Actividades
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MonitorActivityPlansIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ProgramRepository programRepository;

    private Monitor testMonitor;
    private Professor testProfessor;
    private Monitoring testMonitoring1;
    private Monitoring testMonitoring2;

    @BeforeEach
    void setUp() {
        // Limpiar datos anteriores
        activityRepository.deleteAll();
        monitoringRepository.deleteAll();

        // Crear profesor de prueba
        testProfessor = new Professor("TEST_PROF_001");
        testProfessor.setName("Dr. Test");
        testProfessor.setPassword("password123");
        professorRepository.save(testProfessor);

        // Crear monitor de prueba
        testMonitor = new Monitor();
        testMonitor.setCode("TEST_MON_001");
        testMonitor.setName("Ana");
        testMonitor.setLastName("Test");
        monitorRepository.save(testMonitor);

        // Crear curso y programa
        Course course1 = courseRepository.findByName("Programación I")
                .orElseGet(() -> {
                    Course c = new Course();
                    c.setName("Programación I");
                    return courseRepository.save(c);
                });

        Course course2 = courseRepository.findByName("Estructuras de Datos")
                .orElseGet(() -> {
                    Course c = new Course();
                    c.setName("Estructuras de Datos");
                    return courseRepository.save(c);
                });

        Program program = programRepository.findByName("Ingeniería de Sistemas")
                .orElseGet(() -> {
                    Program p = new Program();
                    p.setName("Ingeniería de Sistemas");
                    return programRepository.save(p);
                });

        // Crear monitorías de prueba
        testMonitoring1 = new Monitoring();
        testMonitoring1.setCourse(course1);
        testMonitoring1.setProgram(program);
        testMonitoring1.setProfessor(testProfessor);
        testMonitoring1.setSemester("2024-2");
        testMonitoring1 = monitoringRepository.save(testMonitoring1);

        testMonitoring2 = new Monitoring();
        testMonitoring2.setCourse(course2);
        testMonitoring2.setProgram(program);
        testMonitoring2.setProfessor(testProfessor);
        testMonitoring2.setSemester("2025-2");
        testMonitoring2 = monitoringRepository.save(testMonitoring2);

        // Crear MonitoringMonitor para asignar el monitor a las monitorías
        MonitoringMonitor mm1 = new MonitoringMonitor();
        mm1.setMonitoring(testMonitoring1);
        mm1.setMonitor(testMonitor);
        mm1.setEstadoSeleccion("aprobado");
        testMonitoring1.getMonitoringMonitors().add(mm1);

        MonitoringMonitor mm2 = new MonitoringMonitor();
        mm2.setMonitoring(testMonitoring2);
        mm2.setMonitor(testMonitor);
        mm2.setEstadoSeleccion("seleccionado");
        testMonitoring2.getMonitoringMonitors().add(mm2);

        monitoringRepository.save(testMonitoring1);
        monitoringRepository.save(testMonitoring2);
    }

    @Test
    @DisplayName("HU-017: GET /api/activity-schedule/monitor/{monitorId}/all-plans - Debe retornar 200 OK")
    @WithMockUser(roles = "MONITOR")
    void testGetMonitorActivityPlans_Success() throws Exception {
        // Given: Crear actividades para el monitor
        Activity activity1 = createTestActivity("Apoyo en Clase", testMonitoring1, StateActivity.PENDIENTE, 2.0);
        Activity activity2 = createTestActivity("Tutoría Grupal", testMonitoring2, StateActivity.COMPLETADO, 3.0);
        
        activityRepository.save(activity1);
        activityRepository.save(activity2);

        // When & Then
        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", testMonitor.getCode())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].courseName", hasItem("Programación I")))
                .andExpect(jsonPath("$[*].professorName", hasItem(containsString("Dr. Test"))));
    }

    @Test
    @DisplayName("HU-017: GET - Debe incluir estadísticas correctas en los planes")
    @WithMockUser(roles = "MONITOR")
    void testGetMonitorActivityPlans_IncludesCorrectStatistics() throws Exception {
        // Given: Crear múltiples actividades con diferentes estados
        Activity activity1 = createTestActivity("Act 1", testMonitoring1, StateActivity.PENDIENTE, 2.0);
        Activity activity2 = createTestActivity("Act 2", testMonitoring1, StateActivity.COMPLETADO, 3.0);
        Activity activity3 = createTestActivity("Act 3", testMonitoring1, StateActivity.COMPLETADOT, 1.5);
        Activity activity4 = createTestActivity("Act 4", testMonitoring1, StateActivity.PENDIENTE, 2.5);
        
        activityRepository.save(activity1);
        activityRepository.save(activity2);
        activityRepository.save(activity3);
        activityRepository.save(activity4);

        // When & Then
        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", testMonitor.getCode())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.courseName == 'Programación I')].totalActivities", hasItem(4)))
                .andExpect(jsonPath("$[?(@.courseName == 'Programación I')].pendingActivities", hasItem(2)))
                .andExpect(jsonPath("$[?(@.courseName == 'Programación I')].completedActivities", hasItem(2)))
                .andExpect(jsonPath("$[?(@.courseName == 'Programación I')].totalHours", hasItem(9.0)));
    }

    @Test
    @DisplayName("HU-017: GET - Debe retornar lista vacía para monitor sin actividades")
    @WithMockUser(roles = "MONITOR")
    void testGetMonitorActivityPlans_EmptyForMonitorWithoutActivities() throws Exception {
        // Given: Monitor sin actividades (ya está creado sin actividades)
        
        // When & Then
        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", testMonitor.getCode())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("HU-017: GET - Debe retornar 500 para monitor inexistente")
    @WithMockUser(roles = "MONITOR")
    void testGetMonitorActivityPlans_NonExistentMonitor() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", "NONEXISTENT_999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$", containsString("Error")));
    }

    @Test
    @DisplayName("HU-017: GET - Debe incluir información completa de cada actividad")
    @WithMockUser(roles = "MONITOR")
    void testGetMonitorActivityPlans_IncludesCompleteActivityInfo() throws Exception {
        // Given
        Activity activity = createTestActivity("Tutoría Programación", testMonitoring1, StateActivity.PENDIENTE, 2.5);
        activity.setDescription("Apoyo a estudiantes con dificultades");
        activity.setCategory("Tutoría");
        activity.setPriority("ALTA");
        activityRepository.save(activity);

        // When & Then
        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", testMonitor.getCode())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activities[0].name", is("Tutoría Programación")))
                .andExpect(jsonPath("$[0].activities[0].description", is("Apoyo a estudiantes con dificultades")))
                .andExpect(jsonPath("$[0].activities[0].category", is("Tutoría")))
                .andExpect(jsonPath("$[0].activities[0].priority", is("ALTA")))
                .andExpect(jsonPath("$[0].activities[0].state", is("PENDIENTE")));
    }

    @Test
    @DisplayName("HU-017: GET - Debe combinar monitorías asignadas y actividades directas")
    @WithMockUser(roles = "MONITOR")
    void testGetMonitorActivityPlans_CombinesAssignedAndDirectActivities() throws Exception {
        // Given: Crear actividad directamente asignada al monitor sin estar en monitoringMonitors
        Monitor directMonitor = new Monitor();
        directMonitor.setCode("DIRECT_MON");
        directMonitor.setName("Direct");
        monitorRepository.save(directMonitor);

        Monitoring directMonitoring = new Monitoring();
        directMonitoring.setCourse(testMonitoring1.getCourse());
        directMonitoring.setProgram(testMonitoring1.getProgram());
        directMonitoring.setProfessor(testProfessor);
        directMonitoring.setSemester("2025-1");
        directMonitoring = monitoringRepository.save(directMonitoring);

        Activity directActivity = new Activity();
        directActivity.setName("Actividad Directa");
        directActivity.setState(StateActivity.PENDIENTE);
        directActivity.setMonitoring(directMonitoring);
        directActivity.setMonitor(directMonitor); // Asignada directamente
        directActivity.setFinish(Date.valueOf(LocalDate.now().plusDays(7)));
        activityRepository.save(directActivity);

        // When & Then
        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", directMonitor.getCode())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].activities[*].name", hasItem("Actividad Directa")));
    }

    @Test
    @DisplayName("HU-017: GET - No debe incluir duplicados si monitoría aparece en ambas fuentes")
    @WithMockUser(roles = "MONITOR")
    void testGetMonitorActivityPlans_NoDuplicates() throws Exception {
        // Given: El monitor está tanto en monitoringMonitors como tiene actividades directas
        Activity activity1 = createTestActivity("Act 1", testMonitoring1, StateActivity.PENDIENTE, 2.0);
        Activity activity2 = createTestActivity("Act 2", testMonitoring1, StateActivity.PENDIENTE, 2.0);
        
        activityRepository.save(activity1);
        activityRepository.save(activity2);

        // When & Then
        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", testMonitor.getCode())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(2))));
        
        // Nota: El HashSet en el servicio debe garantizar que no hay duplicados
        // Si hay 2 monitorías, debe retornar máximo 2 planes (sin duplicados)
    }

    // ==================== Helper Methods ====================

    private Activity createTestActivity(String name, Monitoring monitoring, StateActivity state, Double hours) {
        Activity activity = new Activity();
        activity.setName(name);
        activity.setDescription("Descripción de " + name);
        activity.setCategory("Tutoría");
        activity.setState(state);
        activity.setMonitoring(monitoring);
        activity.setMonitor(testMonitor);
        activity.setFinish(Date.valueOf(LocalDate.now().plusDays(7)));
        activity.setDurationHours(BigDecimal.valueOf(hours));
        activity.setPriority("MEDIA");
        return activity;
    }
}

