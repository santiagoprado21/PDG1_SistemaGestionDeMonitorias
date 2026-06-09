package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.ActivityDTO;
import com.pdg.sigma.dto.ActivityRequestDTO;
import com.pdg.sigma.dto.NewActivityRequestDTO;
import com.pdg.sigma.repository.ActivityRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.ProspectRepository;
import com.pdg.sigma.service.ActivityServiceImpl;
import com.pdg.sigma.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityServiceImplTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ActivityServiceImpl activityService;

    private Activity mockActivity;
    private Monitor mockMonitor;
    private Professor mockProfessor;
    private Monitoring mockMonitoring;
    private Course mockCourse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockCourse = new Course();
        mockCourse.setId(1L);
        mockCourse.setName("POO");

        mockMonitoring = new Monitoring();
        mockMonitoring.setId(1L);
        mockMonitoring.setCourse(mockCourse);

        mockMonitor = new Monitor();
        mockMonitor.setCode("M001");
        mockMonitor.setName("Carlos");
        mockMonitor.setLastName("Pérez");

        mockProfessor = new Professor();
        mockProfessor.setId("P001");
        mockProfessor.setName("Dr. Profesor");

        mockActivity = new Activity();
        mockActivity.setId(1);
        mockActivity.setName("Tarea 1");
        mockActivity.setCreation(new Date());
        mockActivity.setFinish(new Date());
        mockActivity.setRoleCreator("P");
        mockActivity.setRoleResponsable("M");
        mockActivity.setDescription("Descripción");
        mockActivity.setMonitoring(mockMonitoring);
        mockActivity.setProfessor(mockProfessor);
        mockActivity.setMonitor(mockMonitor);
        mockActivity.setState(StateActivity.PENDIENTE);
        mockActivity.setSemester("2026-1");
    }

    @Test
    @DisplayName("Debe listar todas las actividades")
    void testFindAll() {
        when(activityRepository.findAll()).thenReturn(List.of(mockActivity));

        List<Activity> result = activityService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe buscar actividad por ID")
    void testFindById() {
        when(activityRepository.findById(1)).thenReturn(Optional.of(mockActivity));

        Optional<Activity> result = activityService.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Tarea 1", result.get().getName());
    }

    @Test
    @DisplayName("Debe guardar actividad entity")
    void testSaveEntity() throws Exception {
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);

        Activity result = activityService.save(mockActivity);

        assertNotNull(result);
        verify(activityRepository, times(1)).save(mockActivity);
    }

    @Test
    @DisplayName("Debe retornar null en count")
    void testCount() {
        assertNull(activityService.count());
    }

    @Test
    @DisplayName("Debe lanzar excepción en update entity")
    void testUpdateEntity() {
        assertThrows(UnsupportedOperationException.class, () -> {
            activityService.update(mockActivity);
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción en delete entity")
    void testDeleteEntity() {
        assertThrows(UnsupportedOperationException.class, () -> {
            activityService.delete(mockActivity);
        });
    }

    @Test
    @DisplayName("Debe lanzar excepción en validate")
    void testValidate() {
        assertThrows(UnsupportedOperationException.class, () -> {
            activityService.validate(mockActivity);
        });
    }

    @Test
    @DisplayName("Debe eliminar actividad por ID si existe")
    void testDeleteByIdExists() throws Exception {
        when(activityRepository.findById(1)).thenReturn(Optional.of(mockActivity));
        doNothing().when(activityRepository).deleteById(1);

        activityService.deleteById(1);

        verify(activityRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Debe fallar al eliminar actividad que no existe")
    void testDeleteByIdNotFound() {
        when(activityRepository.findById(999)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            activityService.deleteById(999);
        });

        assertTrue(exception.getMessage().contains("No se encontró"));
    }

    @Test
    @DisplayName("Debe guardar actividad con DTO - rol P-M")
    void testSaveNewDTO_P_M() throws Exception {
        NewActivityRequestDTO dto = new NewActivityRequestDTO();
        dto.setName("Nueva tarea");
        dto.setFinish(new Date());
        dto.setRoleCreator("P");
        dto.setRoleResponsable("M");
        dto.setDescription("Desc");
        dto.setMonitoringId(1);
        dto.setProfessorId("P001");
        dto.setMonitorId("M001");
        dto.setSemester("2026-1");

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(professorRepository.findById("P001")).thenReturn(Optional.of(mockProfessor));
        when(monitorRepository.findByIdMonitor("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);

        ActivityDTO result = activityService.save(dto);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Debe guardar actividad con DTO - rol M-M")
    void testSaveNewDTO_M_M() throws Exception {
        NewActivityRequestDTO dto = new NewActivityRequestDTO();
        dto.setName("Nueva tarea");
        dto.setFinish(new Date());
        dto.setRoleCreator("M");
        dto.setRoleResponsable("M");
        dto.setDescription("Desc");
        dto.setMonitoringId(1);
        dto.setProfessorId("M001");
        dto.setMonitorId("M001");
        dto.setSemester("2026-1");

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(monitorRepository.findByIdMonitor("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);

        ActivityDTO result = activityService.save(dto);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Debe guardar actividad con DTO - rol M-P")
    void testSaveNewDTO_M_P() throws Exception {
        NewActivityRequestDTO dto = new NewActivityRequestDTO();
        dto.setName("Nueva tarea");
        dto.setFinish(new Date());
        dto.setRoleCreator("M");
        dto.setRoleResponsable("P");
        dto.setDescription("Desc");
        dto.setMonitoringId(1);
        dto.setProfessorId("M001");
        dto.setMonitorId("P001");
        dto.setSemester("2026-1");

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(monitorRepository.findByIdMonitor("M001")).thenReturn(Optional.of(mockMonitor));
        when(professorRepository.findById("P001")).thenReturn(Optional.of(mockProfessor));
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);

        ActivityDTO result = activityService.save(dto);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Debe guardar actividad con DTO - rol P-P")
    void testSaveNewDTO_P_P() throws Exception {
        NewActivityRequestDTO dto = new NewActivityRequestDTO();
        dto.setName("Nueva tarea");
        dto.setFinish(new Date());
        dto.setRoleCreator("P");
        dto.setRoleResponsable("P");
        dto.setDescription("Desc");
        dto.setMonitoringId(1);
        dto.setProfessorId("P001");
        dto.setMonitorId("P001");
        dto.setSemester("2026-1");

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(professorRepository.findById("P001")).thenReturn(Optional.of(mockProfessor));
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);

        ActivityDTO result = activityService.save(dto);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Debe fallar si monitoring no existe en save")
    void testSaveNewDTOMonitoringNotFound() {
        NewActivityRequestDTO dto = new NewActivityRequestDTO();
        dto.setMonitoringId(999);

        when(monitoringRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            activityService.save(dto);
        });

        assertTrue(exception.getMessage().contains("Monitoring not found"));
    }

    @Test
    @DisplayName("Debe actualizar actividad con DTO")
    void testUpdateActivity() throws Exception {
        ActivityRequestDTO dto = new ActivityRequestDTO();
        dto.setId(1);
        dto.setName("Actualizado");
        dto.setDescription("Nueva desc");
        dto.setCategory("Tutoría");
        dto.setState("pendiente");
        dto.setSemester("2026-2");
        dto.setCreation(new Date());
        dto.setFinish(new Date());
        dto.setRoleCreator("P");
        dto.setRoleResponsable("M");
        dto.setMonitorId("M001");
        dto.setDelivey(new Date());

        when(activityRepository.findById(1)).thenReturn(Optional.of(mockActivity));
        when(monitorRepository.findByIdMonitor("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);
        doNothing().when(notificationService).notifyProgressUpdate(any(Activity.class));

        ActivityDTO result = activityService.update(dto);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Debe fallar si actividad no existe en update")
    void testUpdateActivityNotFound() {
        ActivityRequestDTO dto = new ActivityRequestDTO();
        dto.setId(999);

        when(activityRepository.findById(999)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            activityService.update(dto);
        });

        assertTrue(exception.getMessage().contains("Activity not found"));
    }

    @Test
    @DisplayName("Debe fallar si monitor no existe en update")
    void testUpdateActivityMonitorNotFound() {
        ActivityRequestDTO dto = new ActivityRequestDTO();
        dto.setId(1);
        dto.setRoleCreator("P");
        dto.setRoleResponsable("M");
        dto.setMonitorId("INVALID");

        when(activityRepository.findById(1)).thenReturn(Optional.of(mockActivity));
        when(monitorRepository.findByIdMonitor("INVALID")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            activityService.update(dto);
        });

        assertTrue(exception.getMessage().contains("Monitor not found"));
    }

    @Test
    @DisplayName("Debe completar actividad a tiempo - updateState")
    void testUpdateStateOnTime() throws Exception {
        Date futureDate = new Date(System.currentTimeMillis() + 86400000);
        mockActivity.setFinish(futureDate);

        when(activityRepository.findById(1)).thenReturn(Optional.of(mockActivity));
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);
        doNothing().when(notificationService).notifyCompleted(any(Activity.class));

        boolean result = activityService.updateState("1");

        assertTrue(result);
        assertEquals(StateActivity.COMPLETADO, mockActivity.getState());
    }

    @Test
    @DisplayName("Debe fallar si actividad no existe en updateState")
    void testUpdateStateNotFound() {
        when(activityRepository.findById(999)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            activityService.updateState("999");
        });

        assertTrue(exception.getMessage().contains("No se encontró"));
    }

    @Test
    @DisplayName("Debe listar actividades como monitor")
    void testFindAllAsMonitor() throws Exception {
        Prospect prospect = new Prospect();
        prospect.setId("U001");
        prospect.setCode("M001");

        when(prospectRepository.findById("U001")).thenReturn(Optional.of(prospect));
        when(monitorRepository.findByCode("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.findByMonitorAndRoleCreator(mockMonitor, "M")).thenReturn(List.of(mockActivity));
        when(activityRepository.findByMonitorAndRoleResponsable(mockMonitor, "M")).thenReturn(List.of());

        List<ActivityDTO> result = activityService.findAll("U001", "monitor");

        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("Debe listar actividades como profesor")
    void testFindAllAsProfessor() throws Exception {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(mockProfessor));
        when(activityRepository.findByProfessorAndRoleCreator(mockProfessor, "P")).thenReturn(List.of(mockActivity));
        when(activityRepository.findByProfessorAndRoleResponsable(mockProfessor, "P")).thenReturn(List.of());

        List<ActivityDTO> result = activityService.findAll("P001", "professor");

        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("Debe fallar si no hay actividades como monitor")
    void testFindAllAsMonitorNoActivities() {
        Prospect prospect = new Prospect();
        prospect.setId("U001");
        prospect.setCode("M001");

        when(prospectRepository.findById("U001")).thenReturn(Optional.of(prospect));
        when(monitorRepository.findByCode("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.findByMonitorAndRoleCreator(mockMonitor, "M")).thenReturn(List.of());
        when(activityRepository.findByMonitorAndRoleResponsable(mockMonitor, "M")).thenReturn(List.of());

        Exception exception = assertThrows(Exception.class, () -> {
            activityService.findAll("U001", "monitor");
        });

        assertTrue(exception.getMessage().contains("No actividades"));
    }

    @Test
    @DisplayName("Debe fallar si no hay actividades como profesor")
    void testFindAllAsProfessorNoActivities() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(mockProfessor));
        when(activityRepository.findByProfessorAndRoleCreator(mockProfessor, "P")).thenReturn(List.of());
        when(activityRepository.findByProfessorAndRoleResponsable(mockProfessor, "P")).thenReturn(List.of());

        Exception exception = assertThrows(Exception.class, () -> {
            activityService.findAll("P001", "professor");
        });

        assertTrue(exception.getMessage().contains("No actividades"));
    }

    @Test
    @DisplayName("Debe listar actividades asignadas como monitor")
    void testFindAllAsMonitorWithAssigned() throws Exception {
        Activity createdActivity = new Activity();
        createdActivity.setId(1);
        createdActivity.setName("Creada");
        createdActivity.setRoleCreator("M");
        createdActivity.setRoleResponsable("M");
        createdActivity.setMonitoring(mockMonitoring);
        createdActivity.setMonitor(mockMonitor);
        createdActivity.setProfessor(mockProfessor);
        createdActivity.setState(StateActivity.PENDIENTE);
        createdActivity.setCreation(new Date());
        createdActivity.setFinish(new Date());
        createdActivity.setSemester("2026-1");

        Activity assignedActivity = new Activity();
        assignedActivity.setId(2);
        assignedActivity.setName("Asignada");
        assignedActivity.setRoleResponsable("M");
        assignedActivity.setMonitoring(mockMonitoring);
        assignedActivity.setMonitor(mockMonitor);
        assignedActivity.setProfessor(mockProfessor);
        assignedActivity.setState(StateActivity.PENDIENTE);
        assignedActivity.setCreation(new Date());
        assignedActivity.setFinish(new Date());
        assignedActivity.setSemester("2026-1");

        Prospect prospect = new Prospect();
        prospect.setId("U001");
        prospect.setCode("M001");

        when(prospectRepository.findById("U001")).thenReturn(Optional.of(prospect));
        when(monitorRepository.findByCode("M001")).thenReturn(Optional.of(mockMonitor));
        when(activityRepository.findByMonitorAndRoleCreator(mockMonitor, "M")).thenReturn(List.of(createdActivity));
        when(activityRepository.findByMonitorAndRoleResponsable(mockMonitor, "M")).thenReturn(List.of(assignedActivity));

        List<ActivityDTO> result = activityService.findAll("U001", "monitor");

        assertEquals(2, result.size());
    }
}
