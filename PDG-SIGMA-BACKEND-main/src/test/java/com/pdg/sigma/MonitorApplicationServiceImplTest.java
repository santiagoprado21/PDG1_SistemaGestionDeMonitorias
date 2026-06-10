package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitorApplicationDTO;
import com.pdg.sigma.dto.SelectMonitorRequest;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.MonitorApplicationServiceImpl;
import com.pdg.sigma.service.MonitoringRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorApplicationServiceImplTest {

    @Mock
    private MonitorApplicationRepository monitorApplicationRepository;

    @Mock
    private MonitoringRequestRepository monitoringRequestRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private MonitoringRequestService monitoringRequestService;

    @Mock
    private ProspectRepository prospectRepository;

    @InjectMocks
    private MonitorApplicationServiceImpl monitorApplicationService;

    private MonitorApplicationDTO applicationDTO;
    private SelectMonitorRequest selectRequest;
    private MonitoringRequest monitoringRequest;
    private Monitor monitor;
    private MonitorApplication application;
    private MonitorApplication otherApplication;
    private Professor professor;
    private Course course;
    private Prospect prospect;

    @BeforeEach
    void setUp() {
        professor = new Professor();
        professor.setId("PROF001");

        course = new Course();
        course.setId(1L);
        course.setName("Programacion Avanzada");

        monitoringRequest = new MonitoringRequest();
        monitoringRequest.setId(1L);
        monitoringRequest.setProfessor(professor);
        monitoringRequest.setCourse(course);
        monitoringRequest.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);
        monitoringRequest.setRequiredAverageGrade(3.5);
        monitoringRequest.setRequiredCourseGrade(3.0);
        monitoringRequest.setApprovedByHead("JEFE001");

        monitor = new Monitor();
        monitor.setCode("MON001");
        monitor.setIdMonitor("EST001");
        monitor.setName("Juan");
        monitor.setLastName("Perez");
        monitor.setGradeAverage(4.0);
        monitor.setGradeCourse(4.5);

        prospect = new Prospect();
        prospect.setId("EST001");
        prospect.setCode("MON001");
        prospect.setName("Juan");
        prospect.setLastName("Perez");
        prospect.setGradeAverage(4.0);
        prospect.setGradeCourse(4.5);

        application = new MonitorApplication();
        application.setId(1L);
        application.setMonitoringRequest(monitoringRequest);
        application.setMonitor(monitor);
        application.setStatus(ApplicationStatus.POSTULADO);
        application.setApplicationDate(LocalDateTime.now());
        application.setMotivationLetter("Tengo interes en ser monitor");

        otherApplication = new MonitorApplication();
        otherApplication.setId(2L);
        otherApplication.setMonitoringRequest(monitoringRequest);
        otherApplication.setMonitor(monitor);
        otherApplication.setStatus(ApplicationStatus.POSTULADO);

        applicationDTO = new MonitorApplicationDTO();
        applicationDTO.setMonitoringRequestId(1L);
        applicationDTO.setMonitorId("EST001");
        applicationDTO.setMotivationLetter("Tengo interes en ser monitor");

        selectRequest = new SelectMonitorRequest();
        selectRequest.setApplicationId(1L);
        selectRequest.setProfessorId("PROF001");
        selectRequest.setNotes("Buen candidato");
    }

    @Test
    @DisplayName("applyToConvocatoria - Exitoso: crea postulacion correctamente")
    void testApplyToConvocatoria_Success() throws Exception {
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(monitoringRequest));
        when(monitorRepository.findByIdMonitor("EST001")).thenReturn(Optional.of(monitor));
        when(monitorApplicationRepository.findByMonitoringRequestAndMonitor(monitoringRequest, monitor))
                .thenReturn(Optional.empty());
        when(prospectRepository.findById("EST001")).thenReturn(Optional.of(prospect));
        when(monitorApplicationRepository.save(any(MonitorApplication.class))).thenReturn(application);

        MonitorApplication result = monitorApplicationService.applyToConvocatoria(applicationDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(ApplicationStatus.POSTULADO, result.getStatus());
        verify(monitoringRequestRepository, times(2)).findById(1L);
        verify(monitorRepository, times(2)).findByIdMonitor("EST001");
        verify(monitorApplicationRepository).findByMonitoringRequestAndMonitor(monitoringRequest, monitor);
        verify(prospectRepository).findById("EST001");
        verify(monitorApplicationRepository).save(any(MonitorApplication.class));
    }

    @Test
    @DisplayName("applyToConvocatoria - Error: convocatoria no encontrada")
    void testApplyToConvocatoria_RequestNotFound() {
        when(monitoringRequestRepository.findById(999L)).thenReturn(Optional.empty());

        applicationDTO.setMonitoringRequestId(999L);
        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.applyToConvocatoria(applicationDTO));

        assertTrue(exception.getMessage().contains("no encontrada"));
    }

    @Test
    @DisplayName("applyToConvocatoria - Error: convocatoria no abierta")
    void testApplyToConvocatoria_NotOpen() {
        monitoringRequest.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(monitoringRequest));

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.applyToConvocatoria(applicationDTO));

        assertTrue(exception.getMessage().contains("no est"));
    }

    @Test
    @DisplayName("applyToConvocatoria - Error: ya postulado anteriormente")
    void testApplyToConvocatoria_AlreadyApplied() {
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(monitoringRequest));
        when(monitorRepository.findByIdMonitor("EST001")).thenReturn(Optional.of(monitor));
        when(monitorApplicationRepository.findByMonitoringRequestAndMonitor(monitoringRequest, monitor))
                .thenReturn(Optional.of(application));

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.applyToConvocatoria(applicationDTO));

        assertTrue(exception.getMessage().contains("Ya te has postulado"));
    }

    @Test
    @DisplayName("applyToConvocatoria - Error: no cumple requisitos (promedio bajo)")
    void testApplyToConvocatoria_DoesNotMeetRequirements_LowAverage() {
        monitoringRequest.setRequiredAverageGrade(4.5);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(monitoringRequest));
        when(monitorRepository.findByIdMonitor("EST001")).thenReturn(Optional.of(monitor));
        when(monitorApplicationRepository.findByMonitoringRequestAndMonitor(monitoringRequest, monitor))
                .thenReturn(Optional.empty());
        when(prospectRepository.findById("EST001")).thenReturn(Optional.of(prospect));

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.applyToConvocatoria(applicationDTO));

        assertTrue(exception.getMessage().contains("No cumples con los requisitos"));
    }

    @Test
    @DisplayName("getApplicationsByRequest - retorna lista con lazy loading")
    void testGetApplicationsByRequest() {
        List<MonitorApplication> expected = Arrays.asList(application);
        when(monitorApplicationRepository.findByMonitoringRequestId(1L)).thenReturn(expected);

        List<MonitorApplication> result = monitorApplicationService.getApplicationsByRequest(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(application.getId(), result.get(0).getId());
        verify(monitorApplicationRepository).findByMonitoringRequestId(1L);
    }

    @Test
    @DisplayName("getApplicationsByMonitor - retorna lista por monitor")
    void testGetApplicationsByMonitor() {
        List<MonitorApplication> expected = Arrays.asList(application);
        when(monitorApplicationRepository.findByMonitorIdMonitor("EST001")).thenReturn(expected);

        List<MonitorApplication> result = monitorApplicationService.getApplicationsByMonitor("EST001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(application.getId(), result.get(0).getId());
        verify(monitorApplicationRepository).findByMonitorIdMonitor("EST001");
    }

    @Test
    @DisplayName("selectMonitor - Exitoso: selecciona monitor y crea monitoring completa")
    void testSelectMonitor_Success() throws Exception {
        when(monitorApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(monitorApplicationRepository.findByMonitoringRequest(monitoringRequest))
                .thenReturn(Arrays.asList(application, otherApplication));
        when(monitorApplicationRepository.save(any(MonitorApplication.class))).thenAnswer(i -> i.getArgument(0));
        when(monitoringRepository.save(any(Monitoring.class))).thenAnswer(i -> i.getArgument(0));

        monitorApplicationService.selectMonitor(selectRequest);

        assertEquals(ApplicationStatus.SELECCIONADO, application.getStatus());
        assertEquals("Buen candidato", application.getNotes());
        assertEquals(ApplicationStatus.NO_SELECCIONADO, otherApplication.getStatus());
        verify(monitorApplicationRepository, times(2)).save(any(MonitorApplication.class));
        verify(monitoringRequestService).markMonitorSelected(1L);
        verify(monitoringRepository, times(2)).save(any(Monitoring.class));
        verify(monitoringRequestService).markApproved(1L);
    }

    @Test
    @DisplayName("selectMonitor - Error: postulacion no encontrada")
    void testSelectMonitor_ApplicationNotFound() {
        when(monitorApplicationRepository.findById(999L)).thenReturn(Optional.empty());
        selectRequest.setApplicationId(999L);

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.selectMonitor(selectRequest));

        assertTrue(exception.getMessage().contains("no encontrada"));
    }

    @Test
    @DisplayName("selectMonitor - Error: profesor no es dueno de la convocatoria")
    void testSelectMonitor_WrongProfessor() {
        selectRequest.setProfessorId("OTRO_PROF");
        when(monitorApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.selectMonitor(selectRequest));

        assertTrue(exception.getMessage().contains("Solo el profesor due"));
    }

    @Test
    @DisplayName("selectMonitor - Error: convocatoria no esta abierta")
    void testSelectMonitor_RequestNotOpen() {
        monitoringRequest.setStatus(RequestStatus.MONITOR_SELECCIONADO);
        when(monitorApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.selectMonitor(selectRequest));

        assertTrue(exception.getMessage().contains("no est"));
    }

    @Test
    @DisplayName("cancelApplication - Exitoso: cancela postulacion")
    void testCancelApplication_Success() throws Exception {
        monitor.setIdMonitor("EST001");
        application.setMonitor(monitor);
        application.setStatus(ApplicationStatus.POSTULADO);
        when(monitorApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        monitorApplicationService.cancelApplication(1L, "EST001");

        verify(monitorApplicationRepository).delete(application);
    }

    @Test
    @DisplayName("cancelApplication - Error: no es dueno de la postulacion")
    void testCancelApplication_WrongOwner() {
        monitor.setIdMonitor("EST001");
        application.setMonitor(monitor);
        when(monitorApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.cancelApplication(1L, "OTRO_EST"));

        assertTrue(exception.getMessage().contains("Solo puedes cancelar tus propias postulaciones"));
    }

    @Test
    @DisplayName("cancelApplication - Error: postulacion no esta en estado POSTULADO")
    void testCancelApplication_WrongStatus() {
        monitor.setIdMonitor("EST001");
        application.setMonitor(monitor);
        application.setStatus(ApplicationStatus.SELECCIONADO);
        when(monitorApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.cancelApplication(1L, "EST001"));

        assertTrue(exception.getMessage().contains("Solo se pueden cancelar postulaciones en estado POSTULADO"));
    }

    @Test
    @DisplayName("hasApplied - retorna true cuando ya existe postulacion")
    void testHasApplied_True() {
        when(monitorRepository.findByIdMonitor("EST001")).thenReturn(Optional.of(monitor));
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(monitoringRequest));
        when(monitorApplicationRepository.findByMonitoringRequestAndMonitor(monitoringRequest, monitor))
                .thenReturn(Optional.of(application));

        assertTrue(monitorApplicationService.hasApplied(1L, "EST001"));
    }

    @Test
    @DisplayName("hasApplied - retorna false cuando monitor no existe")
    void testHasApplied_False_MonitorNotFound() {
        when(monitorRepository.findByIdMonitor("NOEXISTE")).thenReturn(Optional.empty());

        assertFalse(monitorApplicationService.hasApplied(1L, "NOEXISTE"));
    }

    @Test
    @DisplayName("hasApplied - retorna false cuando convocatoria no existe")
    void testHasApplied_False_RequestNotFound() {
        when(monitorRepository.findByIdMonitor("EST001")).thenReturn(Optional.of(monitor));
        when(monitoringRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertFalse(monitorApplicationService.hasApplied(999L, "EST001"));
    }

    @Test
    @DisplayName("hasApplied - retorna false cuando no hay postulacion existente")
    void testHasApplied_False_NoApplication() {
        when(monitorRepository.findByIdMonitor("EST001")).thenReturn(Optional.of(monitor));
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(monitoringRequest));
        when(monitorApplicationRepository.findByMonitoringRequestAndMonitor(monitoringRequest, monitor))
                .thenReturn(Optional.empty());

        assertFalse(monitorApplicationService.hasApplied(1L, "EST001"));
    }

    @Test
    @DisplayName("meetsRequirements - retorna true cuando cumple todos los requisitos")
    void testMeetsRequirements_True() throws Exception {
        monitoringRequest.setRequiredAverageGrade(3.5);
        monitoringRequest.setRequiredCourseGrade(3.0);
        when(prospectRepository.findById("EST001")).thenReturn(Optional.of(prospect));

        assertTrue(monitorApplicationService.meetsRequirements("EST001", monitoringRequest));
    }

    @Test
    @DisplayName("meetsRequirements - retorna false cuando el promedio es bajo")
    void testMeetsRequirements_False_LowAverage() throws Exception {
        monitoringRequest.setRequiredAverageGrade(4.5);
        when(prospectRepository.findById("EST001")).thenReturn(Optional.of(prospect));

        assertFalse(monitorApplicationService.meetsRequirements("EST001", monitoringRequest));
    }

    @Test
    @DisplayName("meetsRequirements - retorna false cuando la nota del curso es baja")
    void testMeetsRequirements_False_LowCourseGrade() throws Exception {
        monitoringRequest.setRequiredCourseGrade(4.8);
        when(prospectRepository.findById("EST001")).thenReturn(Optional.of(prospect));

        assertFalse(monitorApplicationService.meetsRequirements("EST001", monitoringRequest));
    }

    @Test
    @DisplayName("meetsRequirements - lanza excepcion cuando prospect no existe")
    void testMeetsRequirements_ProspectNotFound() {
        when(prospectRepository.findById("NOEXISTE")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.meetsRequirements("NOEXISTE", monitoringRequest));

        assertTrue(exception.getMessage().contains("no encontrado"));
    }

    @Test
    @DisplayName("meetsRequirements - retorna true cuando no hay requisitos definidos")
    void testMeetsRequirements_True_NoRequirements() throws Exception {
        monitoringRequest.setRequiredAverageGrade(null);
        monitoringRequest.setRequiredCourseGrade(null);
        when(prospectRepository.findById("EST001")).thenReturn(Optional.of(prospect));

        assertTrue(monitorApplicationService.meetsRequirements("EST001", monitoringRequest));
    }

    @Test
    @DisplayName("getAvailableConvocatoriasForMonitor - retorna lista de convocatorias disponibles")
    void testGetAvailableConvocatoriasForMonitor() {
        List<MonitoringRequest> expected = Arrays.asList(monitoringRequest);
        when(monitorApplicationRepository.findAvailableConvocatoriasForMonitor("EST001", 1))
                .thenReturn(expected);

        List<MonitoringRequest> result =
                monitorApplicationService.getAvailableConvocatoriasForMonitor("EST001", 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(monitorApplicationRepository).findAvailableConvocatoriasForMonitor("EST001", 1);
    }

    @Test
    @DisplayName("findAll - retorna todas las postulaciones")
    void testFindAll() {
        List<MonitorApplication> expected = Arrays.asList(application);
        when(monitorApplicationRepository.findAll()).thenReturn(expected);

        List<MonitorApplication> result = monitorApplicationService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(application.getId(), result.get(0).getId());
        verify(monitorApplicationRepository).findAll();
    }

    @Test
    @DisplayName("findById - retorna postulacion cuando existe")
    void testFindById_Found() {
        when(monitorApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        Optional<MonitorApplication> result = monitorApplicationService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(monitorApplicationRepository).findById(1L);
    }

    @Test
    @DisplayName("findById - retorna empty cuando no existe")
    void testFindById_NotFound() {
        when(monitorApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<MonitorApplication> result = monitorApplicationService.findById(999L);

        assertFalse(result.isPresent());
        verify(monitorApplicationRepository).findById(999L);
    }

    @Test
    @DisplayName("save - guarda postulacion exitosamente")
    void testSave() throws Exception {
        when(monitorApplicationRepository.save(application)).thenReturn(application);

        MonitorApplication result = monitorApplicationService.save(application);

        assertNotNull(result);
        assertEquals(application.getId(), result.getId());
        verify(monitorApplicationRepository).save(application);
    }

    @Test
    @DisplayName("update - actualiza postulacion exitosamente")
    void testUpdate_Success() throws Exception {
        application.setId(1L);
        application.setMotivationLetter("Actualizada");
        when(monitorApplicationRepository.existsById(1L)).thenReturn(true);
        when(monitorApplicationRepository.save(application)).thenReturn(application);

        MonitorApplication result = monitorApplicationService.update(application);

        assertNotNull(result);
        assertEquals("Actualizada", result.getMotivationLetter());
        assertNotNull(result.getUpdatedAt());
        verify(monitorApplicationRepository).existsById(1L);
        verify(monitorApplicationRepository).save(application);
    }

    @Test
    @DisplayName("update - lanza excepcion cuando id es nulo")
    void testUpdate_NullId() {
        application.setId(null);

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.update(application));

        assertTrue(exception.getMessage().contains("no existe"));
    }

    @Test
    @DisplayName("update - lanza excepcion cuando postulacion no existe")
    void testUpdate_NotFound() {
        application.setId(999L);
        when(monitorApplicationRepository.existsById(999L)).thenReturn(false);

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.update(application));

        assertTrue(exception.getMessage().contains("no existe"));
    }

    @Test
    @DisplayName("delete - elimina postulacion en estado POSTULADO")
    void testDelete_Postulado_Success() throws Exception {
        application.setStatus(ApplicationStatus.POSTULADO);

        monitorApplicationService.delete(application);

        verify(monitorApplicationRepository).delete(application);
    }

    @Test
    @DisplayName("delete - elimina postulacion en estado NO_SELECCIONADO")
    void testDelete_NoSeleccionado_Success() throws Exception {
        application.setStatus(ApplicationStatus.NO_SELECCIONADO);

        monitorApplicationService.delete(application);

        verify(monitorApplicationRepository).delete(application);
    }

    @Test
    @DisplayName("delete - lanza excepcion cuando postulacion esta SELECCIONADO")
    void testDelete_Selected_ThrowsException() {
        application.setStatus(ApplicationStatus.SELECCIONADO);

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.delete(application));

        assertTrue(exception.getMessage().contains("No se puede eliminar una postulaci"));
    }

    @Test
    @DisplayName("deleteById - elimina postulacion por ID exitosamente")
    void testDeleteById_Success() throws Exception {
        application.setStatus(ApplicationStatus.POSTULADO);
        when(monitorApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        monitorApplicationService.deleteById(1L);

        verify(monitorApplicationRepository).findById(1L);
        verify(monitorApplicationRepository).delete(application);
    }

    @Test
    @DisplayName("deleteById - lanza excepcion cuando postulacion no existe")
    void testDeleteById_NotFound() {
        when(monitorApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.deleteById(999L));

        assertTrue(exception.getMessage().contains("no encontrada"));
    }

    @Test
    @DisplayName("validate - lanza excepcion cuando monitoringRequest es nulo")
    void testValidate_NullRequest() {
        application.setMonitoringRequest(null);

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.validate(application));

        assertTrue(exception.getMessage().contains("convocatoria es obligatoria"));
    }

    @Test
    @DisplayName("validate - lanza excepcion cuando monitor es nulo")
    void testValidate_NullMonitor() {
        application.setMonitor(null);

        Exception exception = assertThrows(Exception.class,
                () -> monitorApplicationService.validate(application));

        assertTrue(exception.getMessage().contains("El monitor (estudiante) es obligatorio"));
    }

    @Test
    @DisplayName("validate - no lanza excepcion cuando todos los campos son validos")
    void testValidate_Success() throws Exception {
        assertDoesNotThrow(() -> monitorApplicationService.validate(application));
    }

    @Test
    @DisplayName("count - retorna el numero total de postulaciones")
    void testCount() {
        when(monitorApplicationRepository.count()).thenReturn(5L);

        Long result = monitorApplicationService.count();

        assertEquals(5L, result);
        verify(monitorApplicationRepository).count();
    }
}
