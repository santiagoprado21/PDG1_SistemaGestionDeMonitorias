package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.ApproveApplicationRequest;
import com.pdg.sigma.dto.MonitorDTO;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.service.MonitoringMonitorServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringMonitorServiceImplTest {

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @InjectMocks
    private MonitoringMonitorServiceImpl monitoringMonitorService;

    private MonitoringMonitor mockRelation;
    private Monitor mockMonitor;
    private Professor mockProfessor;
    private Monitoring mockMonitoring;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockMonitor = new Monitor();
        mockMonitor.setCode("M001");
        mockMonitor.setName("Carlos");
        mockMonitor.setLastName("Pérez");
        mockMonitor.setIdMonitor("idM001");

        mockProfessor = new Professor();
        mockProfessor.setId("P001");
        mockProfessor.setName("Dr. Profesor");

        Course course = new Course();
        course.setName("POO");
        course.setId(1L);

        mockMonitoring = new Monitoring();
        mockMonitoring.setId(1L);
        mockMonitoring.setCourse(course);
        mockMonitoring.setProfessor(mockProfessor);

        mockRelation = new MonitoringMonitor(mockMonitoring, mockMonitor, "seleccionado");
        mockRelation.setId(1L);
    }

    @Test
    @DisplayName("Debe listar todos los monitores por monitoring")
    void testGetMonitorsByMonitoringId() {
        when(monitoringMonitorRepository.findByMonitoringId(1L)).thenReturn(List.of(mockRelation));

        List<MonitorDTO> result = monitoringMonitorService.getMonitorsByMonitoringId(1L);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(m -> "P".equals(m.getRol())));
        assertTrue(result.stream().anyMatch(m -> "M".equals(m.getRol())));
    }

    @Test
    @DisplayName("Debe eliminar relación por monitoring y código")
    void testDeleteRelation() throws Exception {
        doNothing().when(monitoringMonitorRepository).deleteByMonitoringIdAndMonitor_Code(1L, "M001");

        monitoringMonitorService.deleteRelation(1L, "M001");

        verify(monitoringMonitorRepository, times(1)).deleteByMonitoringIdAndMonitor_Code(1L, "M001");
    }

    @Test
    @DisplayName("Debe eliminar relación con trim en código")
    void testDeleteRelationTrimsCode() throws Exception {
        doNothing().when(monitoringMonitorRepository).deleteByMonitoringIdAndMonitor_Code(1L, "M001");

        monitoringMonitorService.deleteRelation(1L, "  M001  ");

        verify(monitoringMonitorRepository, times(1)).deleteByMonitoringIdAndMonitor_Code(1L, "M001");
    }

    @Test
    @DisplayName("Debe actualizar estado de selección")
    void testUpdateApplicantSelectionStatus() {
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(mockRelation));

        monitoringMonitorService.updateApplicantSelectionStatus(1L, "M001", "aprobado");

        assertEquals("aprobado", mockRelation.getEstadoSeleccion());
        verify(monitoringMonitorRepository, times(1)).save(mockRelation);
    }

    @Test
    @DisplayName("Debe fallar si no encuentra relación al actualizar estado")
    void testUpdateApplicantSelectionStatusNotFound() {
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "INVALID"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            monitoringMonitorService.updateApplicantSelectionStatus(1L, "INVALID", "aprobado");
        });
    }

    @Test
    @DisplayName("Debe aprobar postulación")
    void testApproveApplication() throws Exception {
        ApproveApplicationRequest request = new ApproveApplicationRequest();
        request.setMonitoringId(1L);
        request.setMonitorCode("M001");
        request.setComentario("Buen candidato");
        request.setDepartmentHeadId("DH001");

        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(mockRelation));
        when(monitoringMonitorRepository.save(any(MonitoringMonitor.class))).thenReturn(mockRelation);

        monitoringMonitorService.approveApplication(request);

        assertEquals("aprobado", mockRelation.getEstadoSeleccion());
        assertEquals("Buen candidato", mockRelation.getComentarioDecision());
        assertEquals("DH001", mockRelation.getDecididoPor());
        assertNotNull(mockRelation.getFechaDecision());
    }

    @Test
    @DisplayName("Debe rechazar postulación ya procesada al aprobar")
    void testApproveApplicationAlreadyProcessed() {
        ApproveApplicationRequest request = new ApproveApplicationRequest();
        request.setMonitoringId(1L);
        request.setMonitorCode("M001");

        mockRelation.setEstadoSeleccion("aprobado");
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(mockRelation));

        Exception exception = assertThrows(Exception.class, () -> {
            monitoringMonitorService.approveApplication(request);
        });

        assertTrue(exception.getMessage().contains("ya fue procesada"));
    }

    @Test
    @DisplayName("Debe rechazar postulación")
    void testRejectApplication() throws Exception {
        ApproveApplicationRequest request = new ApproveApplicationRequest();
        request.setMonitoringId(1L);
        request.setMonitorCode("M001");
        request.setComentario("No cumple requisitos");
        request.setDepartmentHeadId("DH001");

        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(mockRelation));
        when(monitoringMonitorRepository.save(any(MonitoringMonitor.class))).thenReturn(mockRelation);

        monitoringMonitorService.rejectApplication(request);

        assertEquals("rechazado", mockRelation.getEstadoSeleccion());
        assertEquals("No cumple requisitos", mockRelation.getComentarioDecision());
    }

    @Test
    @DisplayName("Debe fallar al rechazar postulación ya procesada")
    void testRejectApplicationAlreadyProcessed() {
        ApproveApplicationRequest request = new ApproveApplicationRequest();
        request.setMonitoringId(1L);
        request.setMonitorCode("M001");

        mockRelation.setEstadoSeleccion("rechazado");
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(mockRelation));

        Exception exception = assertThrows(Exception.class, () -> {
            monitoringMonitorService.rejectApplication(request);
        });

        assertTrue(exception.getMessage().contains("ya fue procesada"));
    }

    @Test
    @DisplayName("Debe listar todas las relaciones")
    void testFindAll() {
        when(monitoringMonitorRepository.findAll()).thenReturn(List.of(mockRelation));

        List<MonitoringMonitor> result = monitoringMonitorService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe buscar relación por ID")
    void testFindById() {
        when(monitoringMonitorRepository.findById(1L)).thenReturn(Optional.of(mockRelation));

        Optional<MonitoringMonitor> result = monitoringMonitorService.findById(1L);

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Debe guardar relación")
    void testSave() throws Exception {
        when(monitoringMonitorRepository.save(any(MonitoringMonitor.class))).thenReturn(mockRelation);

        MonitoringMonitor result = monitoringMonitorService.save(mockRelation);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Debe actualizar relación")
    void testUpdate() throws Exception {
        when(monitoringMonitorRepository.save(any(MonitoringMonitor.class))).thenReturn(mockRelation);

        MonitoringMonitor result = monitoringMonitorService.update(mockRelation);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Debe eliminar relación")
    void testDelete() throws Exception {
        doNothing().when(monitoringMonitorRepository).delete(any(MonitoringMonitor.class));

        monitoringMonitorService.delete(mockRelation);

        verify(monitoringMonitorRepository, times(1)).delete(mockRelation);
    }

    @Test
    @DisplayName("Debe eliminar relación por ID")
    void testDeleteById() throws Exception {
        doNothing().when(monitoringMonitorRepository).deleteById(1L);

        monitoringMonitorService.deleteById(1L);

        verify(monitoringMonitorRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Debe validar sin hacer nada")
    void testValidate() throws Exception {
        monitoringMonitorService.validate(mockRelation);
    }

    @Test
    @DisplayName("Debe contar relaciones")
    void testCount() {
        when(monitoringMonitorRepository.count()).thenReturn(5L);

        Long result = monitoringMonitorService.count();

        assertEquals(5L, result);
    }
}
