package com.pdg.sigma;

import com.pdg.sigma.domain.Student;
import com.pdg.sigma.domain.StudentCourse;
import com.pdg.sigma.repository.StudentCourseRepository;
import com.pdg.sigma.repository.StudentRepository;
import com.pdg.sigma.service.StudentServiceImpl;
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

class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student mockStudent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockStudent = new Student();
        mockStudent.setCode("S001");
        mockStudent.setName("Juan Pérez");
    }

    @Test
    @DisplayName("Debe listar todos los estudiantes")
    void testFindAll() {
        when(studentRepository.findAll()).thenReturn(List.of(mockStudent));

        List<Student> result = studentService.findAll();

        assertEquals(1, result.size());
        assertEquals("S001", result.get(0).getCode());
    }

    @Test
    @DisplayName("Debe buscar estudiante por ID")
    void testFindById() {
        when(studentRepository.findById("S001")).thenReturn(Optional.of(mockStudent));

        Optional<Student> result = studentService.findById("S001");

        assertTrue(result.isPresent());
        assertEquals("Juan Pérez", result.get().getName());
    }

    @Test
    @DisplayName("Debe guardar estudiante")
    void testSave() {
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        Student result = studentService.save(mockStudent);

        assertNotNull(result);
        verify(studentRepository, times(1)).save(mockStudent);
    }

    @Test
    @DisplayName("Debe actualizar estudiante")
    void testUpdate() {
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        Student result = studentService.update(mockStudent);

        assertNotNull(result);
        verify(studentRepository, times(1)).save(mockStudent);
    }

    @Test
    @DisplayName("Debe eliminar estudiante")
    void testDelete() {
        doNothing().when(studentRepository).delete(any(Student.class));

        studentService.delete(mockStudent);

        verify(studentRepository, times(1)).delete(mockStudent);
    }

    @Test
    @DisplayName("Debe eliminar estudiante por ID")
    void testDeleteById() {
        doNothing().when(studentRepository).deleteById("S001");

        studentService.deleteById("S001");

        verify(studentRepository, times(1)).deleteById("S001");
    }

    @Test
    @DisplayName("Debe obtener estudiantes por curso")
    void testGetStudentsByCourse() {
        StudentCourse sc = new StudentCourse();
        when(studentCourseRepository.findByCourseId(1)).thenReturn(List.of(sc));

        List<StudentCourse> result = studentService.getStudentsByCourse(1);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe contar estudiantes")
    void testCount() {
        when(studentRepository.count()).thenReturn(10L);

        Long result = studentService.count();

        assertEquals(10L, result);
    }
}
