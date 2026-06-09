package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitorDTO;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.MonitorServiceImpl;
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

class MonitorServiceImplTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private MonitorServiceImpl monitorService;

    private Monitor mockMonitor;
    private Monitoring mockMonitoring;
    private MonitoringMonitor mockPostulation;
    private Prospect mockProspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Course course = new Course();
        course.setId(1L);
        course.setName("POO");

        School school = new School();
        school.setName("Facultad de Ingeniería");

        Program program = new Program();
        program.setName("Ingeniería de Sistemas");
        program.setSchool(school);

        mockMonitoring = new Monitoring();
        mockMonitoring.setId(1L);
        mockMonitoring.setCourse(course);
        mockMonitoring.setSchool(school);
        mockMonitoring.setProgram(program);
        mockMonitoring.setAverageGrade(3.0);
        mockMonitoring.setCourseGrade(3.0);

        mockMonitor = new Monitor();
        mockMonitor.setCode("M001");
        mockMonitor.setName("Carlos");
        mockMonitor.setLastName("Pérez");
        mockMonitor.setEmail("carlos@test.com");
        mockMonitor.setSemester(4);
        mockMonitor.setGradeAverage(4.5);
        mockMonitor.setGradeCourse(4.0);

        mockPostulation = new MonitoringMonitor(mockMonitoring, mockMonitor, "seleccionado");

        mockProspect = new Prospect();
        mockProspect.setId("P001");
        mockProspect.setCode("M001");
        mockProspect.setName("Carlos");
        mockProspect.setLastName("Pérez");
        mockProspect.setSemester(4);
        mockProspect.setGradeAverage(4.5);
        mockProspect.setGradeCourse(4.0);
        mockProspect.setEmail("carlos@test.com");
    }

    @Test
    @DisplayName("Debe listar todos los monitores")
    void testFindAll() {
        when(monitorRepository.findAll()).thenReturn(List.of(mockMonitor));

        List<Monitor> result = monitorService.findAll();

        assertEquals(1, result.size());
        assertEquals("M001", result.get(0).getCode());
    }

    @Test
    @DisplayName("Debe listar postulaciones excluyendo aprobados/rechazados")
    void testFindAllNew() {
        when(monitorRepository.findAll()).thenReturn(List.of(mockMonitor));
        when(monitoringMonitorRepository.findByMonitor(mockMonitor)).thenReturn(List.of(mockPostulation));

        List<MonitorDTO> result = monitorService.findAllNew();

        assertEquals(1, result.size());
        assertEquals("M001", result.get(0).getCode());
        assertEquals("POO", result.get(0).getCourse());
    }

    @Test
    @DisplayName("Debe excluir postulaciones aprobadas en findAllNew")
    void testFindAllNewSkipsApproved() {
        MonitoringMonitor approved = new MonitoringMonitor(mockMonitoring, mockMonitor, "aprobado");
        when(monitorRepository.findAll()).thenReturn(List.of(mockMonitor));
        when(monitoringMonitorRepository.findByMonitor(mockMonitor)).thenReturn(List.of(approved));

        List<MonitorDTO> result = monitorService.findAllNew();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Debe guardar nuevo monitor")
    void testSaveNew() throws Exception {
        MonitorDTO dto = new MonitorDTO();
        dto.setUserId("P001");
        dto.setMonitoringId("1");

        when(prospectRepository.findById("P001")).thenReturn(Optional.of(mockProspect));
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(monitorRepository.findByCode("M001")).thenReturn(Optional.empty());
        when(monitorRepository.save(any(Monitor.class))).thenReturn(mockMonitor);

        Monitor result = monitorService.saveNew(dto);

        assertNotNull(result);
        verify(monitorRepository, times(1)).save(any(Monitor.class));
        verify(monitoringMonitorRepository, times(1)).save(any(MonitoringMonitor.class));
    }

    @Test
    @DisplayName("Debe fallar si ya existe postulación duplicada")
    void testSaveNewDuplicate() {
        MonitorDTO dto = new MonitorDTO();
        dto.setUserId("P001");
        dto.setMonitoringId("1");

        when(prospectRepository.findById("P001")).thenReturn(Optional.of(mockProspect));
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(monitorRepository.findByCode("M001")).thenReturn(Optional.of(mockMonitor));
        when(monitoringMonitorRepository.findByMonitoringAndMonitor(mockMonitoring, mockMonitor))
                .thenReturn(Optional.of(mockPostulation));

        Exception exception = assertThrows(Exception.class, () -> {
            monitorService.saveNew(dto);
        });

        assertTrue(exception.getMessage().contains("Ya existe una postulacion"));
    }

    @Test
    @DisplayName("Debe fallar si no cumple requisitos de nota")
    void testSaveNewLowGrades() {
        MonitorDTO dto = new MonitorDTO();
        dto.setUserId("P001");
        dto.setMonitoringId("1");

        mockProspect.setGradeAverage(2.0);
        mockProspect.setGradeCourse(2.0);

        when(prospectRepository.findById("P001")).thenReturn(Optional.of(mockProspect));
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(mockMonitoring));
        when(monitorRepository.findByCode("M001")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            monitorService.saveNew(dto);
        });

        assertTrue(exception.getMessage().contains("No cumple con los requisitos"));
    }

    @Test
    @DisplayName("Debe obtener perfil del monitor")
    void testGetProfile() throws Exception {
        when(monitorRepository.findByIdMonitor("M001")).thenReturn(Optional.of(mockMonitor));
        when(monitoringMonitorRepository.findByMonitor(mockMonitor)).thenReturn(List.of(mockPostulation));

        MonitorDTO result = monitorService.getProfile("M001");

        assertNotNull(result);
        assertEquals("Monitor", result.getRol());
        assertTrue(result.getSchool().contains("Facultad de Ingeniería"));
        assertTrue(result.getProgram().contains("Ingeniería de Sistemas"));
    }

    @Test
    @DisplayName("Debe fallar si monitor no existe")
    void testGetProfileNotFound() {
        when(monitorRepository.findByIdMonitor("INVALID")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            monitorService.getProfile("INVALID");
        });

        assertTrue(exception.getMessage().contains("No existe monitor"));
    }

    @Test
    @DisplayName("Debe eliminar monitor por ID")
    void testDeleteById() throws Exception {
        doNothing().when(monitorRepository).deleteById("M001");

        monitorService.deleteById("M001");

        verify(monitorRepository, times(1)).deleteById("M001");
    }

    @Test
    @DisplayName("Debe fallar al eliminar con ID nulo")
    void testDeleteByIdNull() {
        Exception exception = assertThrows(Exception.class, () -> {
            monitorService.deleteById(null);
        });

        assertTrue(exception.getMessage().contains("ID cannot be null"));
    }

    @Test
    @DisplayName("Debe fallar al eliminar con ID vacío")
    void testDeleteByIdEmpty() {
        Exception exception = assertThrows(Exception.class, () -> {
            monitorService.deleteById("");
        });

        assertTrue(exception.getMessage().contains("ID cannot be null"));
    }
}
