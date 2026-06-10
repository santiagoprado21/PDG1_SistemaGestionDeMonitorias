package com.pdg.sigma;

import com.pdg.sigma.domain.CourseProfessor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.dto.ProfessorDTO;
import com.pdg.sigma.repository.CourseProfessorRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.service.ProfessorServiceImpl;
import com.pdg.sigma.domain.Course;
import com.pdg.sigma.domain.Program;
import com.pdg.sigma.domain.School;
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

class ProfessorServiceImplTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private CourseProfessorRepository courseProfessorRepository;

    @InjectMocks
    private ProfessorServiceImpl professorService;

    private Professor mockProfessor;
    private CourseProfessor mockCourseProfessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        School school = new School();
        school.setName("Facultad de Ingeniería");

        Program program = new Program();
        program.setName("Ingeniería de Sistemas");
        program.setSchool(school);

        Course course = new Course();
        course.setName("POO");
        course.setProgram(program);

        mockProfessor = new Professor();
        mockProfessor.setId("P001");
        mockProfessor.setName("Dr. García");

        mockCourseProfessor = new CourseProfessor();
        mockCourseProfessor.setCourse(course);
        mockCourseProfessor.setProfessor(mockProfessor);
    }

    @Test
    @DisplayName("Debe obtener perfil del profesor correctamente")
    void testGetProfile() throws Exception {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(mockProfessor));
        when(courseProfessorRepository.findByProfessor(mockProfessor)).thenReturn(List.of(mockCourseProfessor));

        ProfessorDTO result = professorService.getProfile("P001");

        assertNotNull(result);
        assertEquals("Profesor", result.getRol());
        assertTrue(result.getSchool().contains("Facultad de Ingeniería"));
        assertTrue(result.getProgram().contains("Ingeniería de Sistemas"));
    }

    @Test
    @DisplayName("Debe fallar si el profesor no existe")
    void testGetProfileNotFound() {
        when(professorRepository.findById("INVALID")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            professorService.getProfile("INVALID");
        });

        assertTrue(exception.getMessage().contains("No existe profesor"));
    }

    @Test
    @DisplayName("Debe fallar si el profesor no tiene cursos asignados")
    void testGetProfileNoCourses() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(mockProfessor));
        when(courseProfessorRepository.findByProfessor(mockProfessor)).thenReturn(List.of());

        Exception exception = assertThrows(Exception.class, () -> {
            professorService.getProfile("P001");
        });

        assertTrue(exception.getMessage().contains("No tiene asignado cursos"));
    }
}
