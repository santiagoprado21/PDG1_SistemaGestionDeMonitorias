package com.pdg.sigma;

import com.pdg.sigma.domain.Course;
import com.pdg.sigma.domain.CourseProfessor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.Program;
import com.pdg.sigma.dto.CourseDTO;
import com.pdg.sigma.repository.CourseProfessorRepository;
import com.pdg.sigma.repository.CourseRepository;
import com.pdg.sigma.service.CourseServiceImpl;
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

class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseProfessorRepository courseProfessorRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course mockCourse;
    private Program mockProgram;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockProgram = new Program();
        mockProgram.setId(1L);
        mockProgram.setName("Ingeniería de Sistemas");

        mockCourse = new Course();
        mockCourse.setId(1L);
        mockCourse.setName("Estructuras de Datos");
        mockCourse.setProgram(mockProgram);
    }

    @Test
    @DisplayName("Debe listar todos los cursos como DTO")
    void testFindAll() {
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse));

        List<CourseDTO> result = courseService.findAll();

        assertEquals(1, result.size());
        assertEquals("Estructuras de Datos", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe buscar cursos por programa")
    void testFindByProgram() {
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse));

        CourseDTO filter = new CourseDTO(null, null, null);
        Program prog = new Program();
        prog.setId(1L);
        filter.setProgram(prog);

        List<CourseDTO> result = courseService.findByProgram(filter);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe buscar cursos por IDs de programa")
    void testFindByProgramIds() {
        when(courseRepository.findByProgramIdIn(List.of(1L))).thenReturn(List.of(mockCourse));

        List<Course> result = courseService.findByProgramIds(List.of(1L));

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe buscar cursos por ID de programa")
    void testFindByProgramId() {
        when(courseRepository.findByProgramId(1L)).thenReturn(List.of(mockCourse));

        List<Course> result = courseService.findByProgramId(1L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe obtener cursos por profesor")
    void testGetCoursesByProfessorId() {
        CourseProfessor cp = new CourseProfessor();
        cp.setCourse(mockCourse);
        when(courseProfessorRepository.findByProfessor(any(Professor.class))).thenReturn(List.of(cp));

        List<Course> result = courseService.getCoursesByProfessorId("P001");

        assertEquals(1, result.size());
        assertEquals("Estructuras de Datos", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe buscar entidad de curso por ID")
    void testFindEntityById() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(mockCourse));

        Optional<Course> result = courseService.findEntityById(1L);

        assertTrue(result.isPresent());
        assertEquals("Estructuras de Datos", result.get().getName());
    }
}
