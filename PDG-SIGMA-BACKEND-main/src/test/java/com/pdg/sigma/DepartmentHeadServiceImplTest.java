package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.DepartmentHeadDTO;
import com.pdg.sigma.dto.PendingApplicationDTO;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.DepartmentHeadServiceImpl;
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

class DepartmentHeadServiceImplTest {

    @Mock
    private DepartmentHeadRepository departmentHeadRepository;

    @Mock
    private HeadProgramRepository headProgramRepository;

    @Mock
    private CourseProfessorRepository courseProfessorRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @InjectMocks
    private DepartmentHeadServiceImpl departmentHeadService;

    private DepartmentHead mockDepartmentHead;
    private HeadProgram mockHeadProgram;
    private Program mockProgram;
    private School mockSchool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockSchool = new School();
        mockSchool.setId(1L);
        mockSchool.setName("Facultad de Ingeniería");

        mockProgram = new Program();
        mockProgram.setId(1L);
        mockProgram.setName("Ingeniería de Sistemas");
        mockProgram.setSchool(mockSchool);

        mockDepartmentHead = new DepartmentHead();
        mockDepartmentHead.setId("DH001");
        mockDepartmentHead.setName("Dr. Jefe");

        mockHeadProgram = new HeadProgram();
        mockHeadProgram.setProgram(mockProgram);
        mockHeadProgram.setDepartmentHead(mockDepartmentHead);
    }

    @Test
    @DisplayName("Debe listar todos los jefes de departamento")
    void testFindAll() {
        when(departmentHeadRepository.findAll()).thenReturn(List.of(mockDepartmentHead));

        List<DepartmentHead> result = departmentHeadService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe obtener perfil del jefe de departamento")
    void testGetProfile() throws Exception {
        when(departmentHeadRepository.findById("DH001")).thenReturn(Optional.of(mockDepartmentHead));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(mockHeadProgram));

        DepartmentHeadDTO result = departmentHeadService.getProfile("DH001");

        assertNotNull(result);
        assertEquals("Jefe de Departamento", result.getRol());
        assertEquals("Facultad de Ingeniería", result.getSchool());
    }

    @Test
    @DisplayName("Debe fallar si jefe no existe")
    void testGetProfileNotFound() {
        when(departmentHeadRepository.findById("INVALID")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            departmentHeadService.getProfile("INVALID");
        });

        assertTrue(exception.getMessage().contains("No existe jefe"));
    }

    @Test
    @DisplayName("Debe obtener programas del jefe")
    void testGetProgramsByDepartmentHead() {
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(mockHeadProgram));

        List<HeadProgram> result = departmentHeadService.getProgramsByDepartmentHead("DH001");

        assertEquals(1, result.size());
        assertEquals("Ingeniería de Sistemas", result.get(0).getProgram().getName());
    }

    @Test
    @DisplayName("Debe obtener profesores del jefe")
    void testGetProfessorsByDepartmentHead() {
        Course course = new Course();
        course.setId(1L);
        course.setProgram(mockProgram);

        Professor professor = new Professor();
        professor.setId("P001");
        professor.setName("Dr. Profesor");

        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(mockHeadProgram));
        when(courseRepository.findByProgramIdIn(List.of(1L))).thenReturn(List.of(course));
        when(courseProfessorRepository.findProfessorsByCourseIds(List.of(1L))).thenReturn(List.of(professor));
        when(professorRepository.findAll()).thenReturn(List.of(professor));

        List<Professor> result = departmentHeadService.getProfessorsByDepartmentHead("DH001");

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(p -> p.getId().equals("P001")));
    }

    @Test
    @DisplayName("Debe obtener postulaciones pendientes")
    void testGetPendingApplications() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setName("POO");
        course.setProgram(mockProgram);

        Professor professor = new Professor();
        professor.setName("Dr. Profesor");

        Monitoring monitoring = new Monitoring();
        monitoring.setId(1L);
        monitoring.setCourse(course);
        monitoring.setProfessor(professor);

        Monitor monitor = new Monitor();
        monitor.setCode("M001");
        monitor.setName("Carlos");
        monitor.setLastName("Pérez");
        monitor.setEmail("carlos@test.com");
        monitor.setGradeAverage(4.5);
        monitor.setGradeCourse(4.0);
        monitor.setSemester(4);

        MonitoringMonitor mm = new MonitoringMonitor(monitoring, monitor, "seleccionado");
        mm.setId(1L);

        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(mockHeadProgram));
        when(courseRepository.findByProgramIdIn(List.of(1L))).thenReturn(List.of(course));
        when(monitoringRepository.findByCourse(course)).thenReturn(Optional.of(monitoring));
        when(monitoringMonitorRepository.findByMonitoring(monitoring)).thenReturn(List.of(mm));

        List<PendingApplicationDTO> result = departmentHeadService.getPendingApplications("DH001");

        assertEquals(1, result.size());
        assertEquals("POO", result.get(0).getCourseName());
    }

    @Test
    @DisplayName("Debe saltar postulaciones no seleccionadas en pendientes")
    void testGetPendingApplicationsSkipsNonSelected() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setName("POO");
        course.setProgram(mockProgram);

        Monitoring monitoring = new Monitoring();
        monitoring.setId(1L);
        monitoring.setCourse(course);

        Monitor monitor = new Monitor();
        monitor.setCode("M001");
        monitor.setName("Carlos");
        monitor.setLastName("Pérez");

        MonitoringMonitor mm = new MonitoringMonitor(monitoring, monitor, "no seleccionado");

        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(mockHeadProgram));
        when(courseRepository.findByProgramIdIn(List.of(1L))).thenReturn(List.of(course));
        when(monitoringRepository.findByCourse(course)).thenReturn(Optional.of(monitoring));
        when(monitoringMonitorRepository.findByMonitoring(monitoring)).thenReturn(List.of(mm));

        List<PendingApplicationDTO> result = departmentHeadService.getPendingApplications("DH001");

        assertTrue(result.isEmpty());
    }
}
