package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.UpdateRequestDTO;
import com.pdg.sigma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSyncServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private CourseProfessorRepository courseProfessorRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DepartmentHeadServiceImpl departmentHeadService;

    @InjectMocks
    private DataSyncService dataSyncService;

    private Professor sampleProfessor;
    private Course sampleCourse;
    private CourseProfessor sampleRelation;

    @BeforeEach
    void setUp() {
        sampleProfessor = new Professor();
        sampleProfessor.setId("P001");
        sampleProfessor.setName("Dr. Juan");

        Program program = new Program();
        program.setId(1L);
        program.setName("Ing. Sistemas");

        sampleCourse = new Course();
        sampleCourse.setId(101L);
        sampleCourse.setName("Programación I");
        sampleCourse.setProgram(program);

        sampleRelation = new CourseProfessor();
        sampleRelation.setCourse(sampleCourse);
        sampleRelation.setProfessor(sampleProfessor);
    }

    private void stubProfessorSyncDoesNothing() {
        lenient().when(restTemplate.getForObject(anyString(), eq(Professor.class))).thenReturn(null);
    }

    // ---- syncData with professorId ----

    @Test
    void syncData_withProfessorId_sameSemester() {
        stubProfessorSyncDoesNothing();
        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setProfessorId("P001");
        request.setUpdateType("sameSemester");
        request.setRemoveMonitors(false);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(restTemplate.getForObject(anyString(), eq(Course[].class)))
                .thenReturn(new Course[]{sampleCourse});
        when(courseProfessorRepository.findByProfessor(sampleProfessor))
                .thenReturn(List.of(sampleRelation));

        String result = dataSyncService.syncData(request);

        assertEquals("Actualización completada con éxito.", result);
        verify(professorRepository).findById("P001");
    }

    @Test
    void syncData_withProfessorId_notFound_throwsException() {
        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setProfessorId("P999");

        when(professorRepository.findById("P999")).thenReturn(Optional.empty());

        Exception e = assertThrows(RuntimeException.class, () -> dataSyncService.syncData(request));
        assertTrue(e.getMessage().contains("no existe"));
    }

    @Test
    void syncData_withProfessorId_apiReturnsNull_continues() {
        stubProfessorSyncDoesNothing();
        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setProfessorId("P001");
        request.setUpdateType("sameSemester");

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(restTemplate.getForObject(anyString(), eq(Course[].class)))
                .thenReturn(null);

        String result = dataSyncService.syncData(request);

        assertEquals("Actualización completada con éxito.", result);
    }

    // ---- syncData with departmentHeadId ----

    @Test
    void syncData_withDepartmentHeadId() {
        stubProfessorSyncDoesNothing();
        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setDepartmentHeadId("H001");
        request.setUpdateType("sameSemester");

        when(departmentHeadService.getProfessorsByDepartmentHead("H001"))
                .thenReturn(List.of(sampleProfessor));
        when(restTemplate.getForObject(anyString(), eq(Course[].class)))
                .thenReturn(new Course[]{sampleCourse});
        when(courseProfessorRepository.findByProfessor(sampleProfessor))
                .thenReturn(List.of(sampleRelation));

        String result = dataSyncService.syncData(request);

        assertEquals("Actualización completada con éxito.", result);
    }

    // ---- syncData with neither professorId nor departmentHeadId (all) ----

    @Test
    void syncData_allProfessors() {
        stubProfessorSyncDoesNothing();
        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setUpdateType("sameSemester");

        when(professorRepository.findAll()).thenReturn(List.of(sampleProfessor));
        when(restTemplate.getForObject(anyString(), eq(Course[].class)))
                .thenReturn(new Course[]{sampleCourse});
        when(courseProfessorRepository.findByProfessor(sampleProfessor))
                .thenReturn(List.of(sampleRelation));

        String result = dataSyncService.syncData(request);

        assertEquals("Actualización completada con éxito.", result);
    }

    // ---- syncData with newSemester update type ----

    @Test
    void syncData_newSemester_updatesCourses() {
        stubProfessorSyncDoesNothing();
        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setProfessorId("P001");
        request.setUpdateType("newSemester");

        Course newCourse = new Course();
        newCourse.setId(200L);
        newCourse.setName("Nuevo Curso");
        newCourse.setProgram(sampleCourse.getProgram());

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(restTemplate.getForObject(anyString(), eq(Course[].class)))
                .thenReturn(new Course[]{newCourse});
        when(courseProfessorRepository.findByProfessor(sampleProfessor))
                .thenReturn(List.of(sampleRelation));
        when(courseRepository.findById(200L)).thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class))).thenReturn(newCourse);

        String result = dataSyncService.syncData(request);

        assertEquals("Actualización completada con éxito.", result);
        verify(courseProfessorRepository).save(any(CourseProfessor.class));
    }

    @Test
    void syncData_newSemester_courseAlreadyExists() {
        stubProfessorSyncDoesNothing();
        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setProfessorId("P001");
        request.setUpdateType("newSemester");

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(restTemplate.getForObject(anyString(), eq(Course[].class)))
                .thenReturn(new Course[]{sampleCourse});
        when(courseProfessorRepository.findByProfessor(sampleProfessor))
                .thenReturn(List.of(sampleRelation));
        when(courseRepository.findById(101L)).thenReturn(Optional.of(sampleCourse));

        String result = dataSyncService.syncData(request);

        assertEquals("Actualización completada con éxito.", result);
        verify(courseRepository, never()).save(any(Course.class));
    }

    // ---- sameSemester with course name update ----

    @Test
    void syncData_sameSemester_updatesCourseName() {
        stubProfessorSyncDoesNothing();
        Course updatedCourse = new Course();
        updatedCourse.setId(101L);
        updatedCourse.setName("Programación I (Actualizado)");
        updatedCourse.setProgram(sampleCourse.getProgram());

        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setProfessorId("P001");
        request.setUpdateType("sameSemester");
        request.setRemoveMonitors(false);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(restTemplate.getForObject(anyString(), eq(Course[].class)))
                .thenReturn(new Course[]{updatedCourse});
        when(courseProfessorRepository.findByProfessor(sampleProfessor))
                .thenReturn(List.of(sampleRelation));

        String result = dataSyncService.syncData(request);

        assertEquals("Actualización completada con éxito.", result);
        verify(courseRepository).save(sampleCourse);
        assertEquals("Programación I (Actualizado)", sampleCourse.getName());
    }

    // ---- syncProfessors ----

    @Test
    void syncProfessors_nullId_returnsEarly() {
        dataSyncService.syncProfessors(null);
        dataSyncService.syncProfessors("");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void syncProfessors_validId_createsNewProfessor() {
        Professor externalProf = new Professor();
        externalProf.setId("P002");
        externalProf.setName("Dr. Nuevo");

        when(restTemplate.getForObject(anyString(), eq(Professor.class)))
                .thenReturn(externalProf);
        when(professorRepository.existsById("P002")).thenReturn(false);

        dataSyncService.syncProfessors("P002");

        verify(professorRepository).save(externalProf);
    }

    @Test
    void syncProfessors_validId_updatesExistingProfessor() {
        Professor externalProf = new Professor();
        externalProf.setId("P001");
        externalProf.setName("Dr. Actualizado");

        when(restTemplate.getForObject(anyString(), eq(Professor.class)))
                .thenReturn(externalProf);
        when(professorRepository.existsById("P001")).thenReturn(true);

        dataSyncService.syncProfessors("P001");

        verify(professorRepository).save(externalProf);
    }

    @Test
    void syncProfessors_apiReturnsNull_doesNothing() {
        when(restTemplate.getForObject(anyString(), eq(Professor.class)))
                .thenReturn(null);

        dataSyncService.syncProfessors("P001");

        verify(professorRepository, never()).save(any());
    }

    // ---- findCourseById helper (tested through sameSemester path) ----

    @Test
    void syncData_sameSemester_courseNotFound_doesNothing() {
        stubProfessorSyncDoesNothing();
        UpdateRequestDTO request = new UpdateRequestDTO();
        request.setProfessorId("P001");
        request.setUpdateType("sameSemester");

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        Course unrelatedCourse = new Course();
        unrelatedCourse.setId(999L);
        unrelatedCourse.setName("Otro");
        when(restTemplate.getForObject(anyString(), eq(Course[].class)))
                .thenReturn(new Course[]{unrelatedCourse});
        when(courseProfessorRepository.findByProfessor(sampleProfessor))
                .thenReturn(List.of(sampleRelation));

        String result = dataSyncService.syncData(request);

        assertEquals("Actualización completada con éxito.", result);
        verify(courseRepository, never()).save(any());
    }
}
