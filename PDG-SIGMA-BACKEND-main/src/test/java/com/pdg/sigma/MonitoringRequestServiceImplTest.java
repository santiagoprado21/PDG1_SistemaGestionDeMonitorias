package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitoringRequestDTO;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.MonitoringRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringRequestServiceImplTest {

    @Mock
    private MonitoringRequestRepository monitoringRequestRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private MonitorApplicationRepository monitorApplicationRepository;

    @Mock
    private CourseProfessorRepository courseProfessorRepository;

    @Mock
    private DepartmentBudgetRepository departmentBudgetRepository;

    @Mock
    private HeadProgramRepository headProgramRepository;

    @Mock
    private DepartmentHeadRepository departmentHeadRepository;

    @InjectMocks
    private MonitoringRequestServiceImpl service;

    private Professor professor;
    private Course course;
    private School school;
    private Program program;
    private MonitoringRequest request;
    private MonitoringRequestDTO dto;
    private Date startDate;
    private Date finishDate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        school = new School();
        school.setId(1L);
        school.setName("Facultad de Ingeniería");

        program = new Program();
        program.setId(1L);
        program.setName("Ingeniería de Sistemas");
        program.setSchool(school);

        professor = new Professor();
        professor.setId("P001");
        professor.setName("Dr. Pérez");
        professor.setPassword("pass");

        course = new Course();
        course.setId(100L);
        course.setName("Programación I");
        course.setProgram(program);

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 15, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDate = cal.getTime();
        cal.set(2026, Calendar.JUNE, 15, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        finishDate = cal.getTime();

        dto = new MonitoringRequestDTO(
                "P001", 100L, 1, 1L,
                20, "Necesito un monitor para el curso",
                "2026-1", startDate, finishDate,
                4.2, 4.5, 15000.0
        );

        request = new MonitoringRequest();
        request.setId(1L);
        request.setProfessor(professor);
        request.setCourse(course);
        request.setSchool(school);
        request.setProgram(program);
        request.setRequestedHours(20);
        request.setJustification("Necesito un monitor");
        request.setSemester("2026-1");
        request.setStartDate(startDate);
        request.setFinishDate(finishDate);
        request.setRequiredAverageGrade(4.2);
        request.setRequiredCourseGrade(4.5);
        request.setHourlyRate(15000.0);
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== createConvocatoria ====================

    @Test
    @DisplayName("createConvocatoria: debe crear convocatoria exitosamente")
    void testCreateConvocatoria_Success() throws Exception {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());
        when(monitoringRequestRepository.save(any(MonitoringRequest.class))).thenAnswer(i -> i.getArgument(0));

        MonitoringRequest result = service.createConvocatoria(dto);

        assertNotNull(result);
        assertEquals(professor, result.getProfessor());
        assertEquals(course, result.getCourse());
        assertEquals(20, result.getRequestedHours());
        assertEquals(RequestStatus.PENDIENTE_APROBACION_JEFE, result.getStatus());

        verify(professorRepository, times(2)).findById("P001");
        verify(courseRepository).findById(100L);
        verify(schoolRepository).findById(1L);
        verify(programRepository, times(2)).findById(1L);
        verify(courseProfessorRepository).findByProfessor(professor);
        verify(monitoringRequestRepository).findByProfessorAndCourseAndSemester(professor, course, "2026-1");
        verify(departmentBudgetRepository).findByProgramAndSemester(program, "2026-1");
        verify(monitoringRequestRepository).save(any(MonitoringRequest.class));
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando el profesor no existe")
    void testCreateConvocatoria_ProfessorNotFound() {
        when(professorRepository.findById("P001")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("Profesor no encontrado con ID: P001", ex.getMessage());
        verify(professorRepository).findById("P001");
        verify(courseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando el curso no existe")
    void testCreateConvocatoria_CourseNotFound() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("Curso no encontrado con ID: 100", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando la facultad no existe")
    void testCreateConvocatoria_SchoolNotFound() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("Facultad no encontrada con ID: 1", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando el programa no existe")
    void testCreateConvocatoria_ProgramNotFound() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("Programa no encontrado con ID: 1", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando el profesor no tiene permiso")
    void testCreateConvocatoria_NoPermission() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor)).thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("El profesor no tiene permiso para crear convocatoria en este curso", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando ya existe una convocatoria activa")
    void testCreateConvocatoria_ExistingActiveConvocatoria() {
        MonitoringRequest existing = new MonitoringRequest();
        existing.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.of(existing));

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("Ya existe una convocatoria activa para este curso en el semestre 2026-1", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe permitir crear cuando la convocatoria existente está RECHAZADA")
    void testCreateConvocatoria_ExistingRejectedAllowsCreation() throws Exception {
        MonitoringRequest existing = new MonitoringRequest();
        existing.setStatus(RequestStatus.RECHAZADA);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.of(existing));
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());
        when(monitoringRequestRepository.save(any(MonitoringRequest.class))).thenAnswer(i -> i.getArgument(0));

        MonitoringRequest result = service.createConvocatoria(dto);
        assertNotNull(result);
        verify(monitoringRequestRepository).save(any(MonitoringRequest.class));
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando no hay presupuesto disponible")
    void testCreateConvocatoria_BudgetNotAvailable() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 10)));
        MonitoringRequest existingUsed = new MonitoringRequest();
        existingUsed.setRequestedHours(15);
        existingUsed.setStatus(RequestStatus.APROBADA);
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of(existingUsed));

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("No hay presupuesto disponible para las horas solicitadas", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando las horas solicitadas son nulas")
    void testCreateConvocatoria_NullRequestedHours() {
        dto.setRequestedHours(0);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("Las horas solicitadas deben ser mayores a 0", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando las horas solicitadas son <= 0")
    void testCreateConvocatoria_RequestedHoursZeroOrNegative() {
        dto.setRequestedHours(-1);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("Las horas solicitadas deben ser mayores a 0", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando la justificación es nula")
    void testCreateConvocatoria_NullJustification() {
        dto.setJustification(null);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("La justificación es obligatoria", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando la justificación está vacía")
    void testCreateConvocatoria_EmptyJustification() {
        dto.setJustification("   ");

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("La justificación es obligatoria", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando las fechas son nulas")
    void testCreateConvocatoria_NullDates() {
        dto.setStartDate(null);
        dto.setFinishDate(null);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("Las fechas de inicio y fin son obligatorias", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando startDate es después de finishDate")
    void testCreateConvocatoria_StartDateAfterFinishDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JUNE, 15, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        dto.setStartDate(cal.getTime());
        cal.set(2026, Calendar.FEBRUARY, 15, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        dto.setFinishDate(cal.getTime());

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("La fecha de inicio debe ser anterior a la fecha de fin", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando el promedio requerido es menor a 4.0")
    void testCreateConvocatoria_RequiredAverageGradeTooLow() {
        dto.setRequiredAverageGrade(3.5);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("El promedio requerido debe ser mínimo 4.0", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando la nota del curso requerida es menor a 4.0")
    void testCreateConvocatoria_RequiredCourseGradeTooLow() {
        dto.setRequiredCourseGrade(3.0);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertEquals("La nota del curso requerida debe ser mínimo 4.0", ex.getMessage());
    }

    @Test
    @DisplayName("createConvocatoria: debe lanzar excepción cuando la fecha de inicio está antes del período académico")
    void testCreateConvocatoria_StartDateBeforeSemester() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 15, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        dto.setStartDate(cal.getTime());
        dto.setFinishDate(finishDate);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));
        when(monitoringRequestRepository.findByProfessorAndCourseAndSemester(professor, course, "2026-1"))
                .thenReturn(Optional.empty());
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());

        Exception ex = assertThrows(Exception.class, () -> service.createConvocatoria(dto));
        assertTrue(ex.getMessage().contains("La fecha de inicio debe ser igual o posterior al inicio del período académico"));
    }

    // ==================== findOpenConvocatorias ====================

    @Test
    @DisplayName("findOpenConvocatorias: debe retornar lista de convocatorias abiertas")
    void testFindOpenConvocatorias() {
        when(monitoringRequestRepository.findAllOpenConvocatorias()).thenReturn(List.of(request));

        List<MonitoringRequest> result = service.findOpenConvocatorias();

        assertEquals(1, result.size());
        assertEquals(request, result.get(0));
        verify(monitoringRequestRepository).findAllOpenConvocatorias();
    }

    // ==================== findOpenConvocatoriasByProgram ====================

    @Test
    @DisplayName("findOpenConvocatoriasByProgram: debe retornar convocatorias abiertas por programa")
    void testFindOpenConvocatoriasByProgram() {
        when(monitoringRequestRepository.findOpenConvocatoriasByProgram(1)).thenReturn(List.of(request));

        List<MonitoringRequest> result = service.findOpenConvocatoriasByProgram(1);

        assertEquals(1, result.size());
        verify(monitoringRequestRepository).findOpenConvocatoriasByProgram(1);
    }

    // ==================== findByProfessor ====================

    @Test
    @DisplayName("findByProfessor: debe retornar convocatorias del profesor")
    void testFindByProfessor_Found() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(monitoringRequestRepository.findByProfessor(professor)).thenReturn(List.of(request));

        List<MonitoringRequest> result = service.findByProfessor("P001");

        assertEquals(1, result.size());
        verify(professorRepository).findById("P001");
        verify(monitoringRequestRepository).findByProfessor(professor);
    }

    @Test
    @DisplayName("findByProfessor: debe retornar lista vacía cuando el profesor no existe")
    void testFindByProfessor_NotFound() {
        when(professorRepository.findById("P999")).thenReturn(Optional.empty());

        List<MonitoringRequest> result = service.findByProfessor("P999");

        assertTrue(result.isEmpty());
        verify(professorRepository).findById("P999");
        verify(monitoringRequestRepository, never()).findByProfessor(any());
    }

    // ==================== findPendingApprovalForDepartmentHead ====================

    @Test
    @DisplayName("findPendingApprovalForDepartmentHead: debe lanzar excepción cuando el jefe no existe")
    void testFindPendingApprovalForDepartmentHead_HeadNotFound() {
        when(departmentHeadRepository.findById("DH001")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> service.findPendingApprovalForDepartmentHead("DH001"));
        assertEquals("Jefe de departamento no encontrado", ex.getMessage());
    }

    @Test
    @DisplayName("findPendingApprovalForDepartmentHead: debe retornar lista vacía cuando no hay programas")
    void testFindPendingApprovalForDepartmentHead_NoPrograms() throws Exception {
        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");
        when(departmentHeadRepository.findById("DH001")).thenReturn(Optional.of(head));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of());

        List<MonitoringRequest> result = service.findPendingApprovalForDepartmentHead("DH001");

        assertTrue(result.isEmpty());
        verify(headProgramRepository).findByDepartmentHeadId("DH001");
    }

    @Test
    @DisplayName("findPendingApprovalForDepartmentHead: debe retornar convocatorias pendientes")
    void testFindPendingApprovalForDepartmentHead_Success() throws Exception {
        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");
        head.setName("Dr. Jefe");

        Program program2 = new Program();
        program2.setId(2L);

        HeadProgram hp = new HeadProgram();
        hp.setId(1);
        hp.setDepartmentHead(head);
        hp.setProgram(program);

        MonitoringRequest pending1 = new MonitoringRequest();
        pending1.setId(1L);
        pending1.setProgram(program);
        pending1.setStatus(RequestStatus.PENDIENTE_APROBACION);

        MonitoringRequest pending2 = new MonitoringRequest();
        pending2.setId(2L);
        pending2.setProgram(program2);
        pending2.setStatus(RequestStatus.PENDIENTE_APROBACION);

        when(departmentHeadRepository.findById("DH001")).thenReturn(Optional.of(head));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(hp));
        when(monitoringRequestRepository.findPendingApprovalByProgram(1)).thenReturn(List.of(pending1, pending2));
        when(monitoringRequestRepository.findPendingApprovalByProgram(program2.getId().intValue())).thenReturn(List.of());

        List<MonitoringRequest> result = service.findPendingApprovalForDepartmentHead("DH001");

        assertEquals(2, result.size());
    }

    // ==================== cancelConvocatoria ====================

    @Test
    @DisplayName("cancelConvocatoria: debe cancelar la convocatoria exitosamente")
    void testCancelConvocatoria_Success() throws Exception {
        request.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        service.cancelConvocatoria(1L, "P001");

        assertEquals(RequestStatus.CANCELADA, request.getStatus());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("cancelConvocatoria: debe lanzar excepción cuando la convocatoria no existe")
    void testCancelConvocatoria_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.cancelConvocatoria(99L, "P001"));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("cancelConvocatoria: debe lanzar excepción cuando otro profesor intenta cancelar")
    void testCancelConvocatoria_WrongProfessor() {
        request.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        Exception ex = assertThrows(Exception.class, () -> service.cancelConvocatoria(1L, "OTRO_PROF"));
        assertEquals("Solo el profesor que creó la convocatoria puede cancelarla", ex.getMessage());
    }

    @Test
    @DisplayName("cancelConvocatoria: debe lanzar excepción cuando la convocatoria no está ABIERTA")
    void testCancelConvocatoria_WrongStatus() {
        request.setStatus(RequestStatus.MONITOR_SELECCIONADO);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        Exception ex = assertThrows(Exception.class, () -> service.cancelConvocatoria(1L, "P001"));
        assertEquals("Solo se pueden cancelar convocatorias en estado ABIERTA", ex.getMessage());
    }

    // ==================== markMonitorSelected ====================

    @Test
    @DisplayName("markMonitorSelected: debe marcar monitor seleccionado exitosamente")
    void testMarkMonitorSelected_Success() throws Exception {
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        service.markMonitorSelected(1L);

        assertEquals(RequestStatus.MONITOR_SELECCIONADO, request.getStatus());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("markMonitorSelected: debe lanzar excepción cuando no se encuentra la convocatoria")
    void testMarkMonitorSelected_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.markMonitorSelected(99L));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    // ==================== markPendingApproval ====================

    @Test
    @DisplayName("markPendingApproval: debe marcar pendiente de aprobación exitosamente")
    void testMarkPendingApproval_Success() throws Exception {
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        service.markPendingApproval(1L);

        assertEquals(RequestStatus.PENDIENTE_APROBACION, request.getStatus());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("markPendingApproval: debe lanzar excepción cuando no se encuentra la convocatoria")
    void testMarkPendingApproval_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.markPendingApproval(99L));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    // ==================== markApproved ====================

    @Test
    @DisplayName("markApproved: debe aprobar la convocatoria exitosamente")
    void testMarkApproved_Success() throws Exception {
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        service.markApproved(1L);

        assertEquals(RequestStatus.APROBADA, request.getStatus());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("markApproved: debe lanzar excepción cuando no se encuentra la convocatoria")
    void testMarkApproved_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.markApproved(99L));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    // ==================== markRejected ====================

    @Test
    @DisplayName("markRejected: debe rechazar la convocatoria exitosamente")
    void testMarkRejected_Success() throws Exception {
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        service.markRejected(1L);

        assertEquals(RequestStatus.RECHAZADA, request.getStatus());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("markRejected: debe lanzar excepción cuando no se encuentra la convocatoria")
    void testMarkRejected_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.markRejected(99L));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    // ==================== validateProfessorPermission ====================

    @Test
    @DisplayName("validateProfessorPermission: debe retornar true cuando el profesor tiene permiso")
    void testValidateProfessorPermission_True() throws Exception {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(1, course, professor)));

        boolean result = service.validateProfessorPermission("P001", 100L);

        assertTrue(result);
    }

    @Test
    @DisplayName("validateProfessorPermission: debe retornar false cuando el profesor no existe")
    void testValidateProfessorPermission_ProfessorNotFound() throws Exception {
        when(professorRepository.findById("P999")).thenReturn(Optional.empty());

        boolean result = service.validateProfessorPermission("P999", 100L);

        assertFalse(result);
        verify(courseProfessorRepository, never()).findByProfessor(any());
    }

    @Test
    @DisplayName("validateProfessorPermission: debe retornar false cuando el profesor no está asignado al curso")
    void testValidateProfessorPermission_NotAssigned() throws Exception {
        Course otherCourse = new Course();
        otherCourse.setId(999L);
        when(professorRepository.findById("P001")).thenReturn(Optional.of(professor));
        when(courseProfessorRepository.findByProfessor(professor))
                .thenReturn(List.of(new CourseProfessor(2, otherCourse, professor)));

        boolean result = service.validateProfessorPermission("P001", 100L);

        assertFalse(result);
    }

    // ==================== validateBudgetAvailability ====================

    @Test
    @DisplayName("validateBudgetAvailability: debe retornar true cuando no hay presupuesto configurado")
    void testValidateBudgetAvailability_NoBudgetConfig() throws Exception {
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.empty());

        boolean result = service.validateBudgetAvailability(1L, "2026-1", 20);

        assertTrue(result);
    }

    @Test
    @DisplayName("validateBudgetAvailability: debe retornar true cuando hay presupuesto suficiente")
    void testValidateBudgetAvailability_Available() throws Exception {
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        DepartmentBudget budget = new DepartmentBudget(program, "2026-1", 100);
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(budget));
        MonitoringRequest used = new MonitoringRequest();
        used.setRequestedHours(30);
        used.setStatus(RequestStatus.APROBADA);
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of(used));

        boolean result = service.validateBudgetAvailability(1L, "2026-1", 20);

        assertTrue(result);
    }

    @Test
    @DisplayName("validateBudgetAvailability: debe retornar false cuando se excede el presupuesto")
    void testValidateBudgetAvailability_Exceeded() throws Exception {
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        DepartmentBudget budget = new DepartmentBudget(program, "2026-1", 50);
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(budget));
        MonitoringRequest used = new MonitoringRequest();
        used.setRequestedHours(40);
        used.setStatus(RequestStatus.APROBADA);
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of(used));

        boolean result = service.validateBudgetAvailability(1L, "2026-1", 20);

        assertFalse(result);
    }

    @Test
    @DisplayName("validateBudgetAvailability: debe retornar false cuando el programa no existe")
    void testValidateBudgetAvailability_ProgramNotFound() throws Exception {
        when(programRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = service.validateBudgetAvailability(99L, "2026-1", 20);

        assertFalse(result);
    }

    // ==================== getApplicationCount ====================

    @Test
    @DisplayName("getApplicationCount: debe retornar el conteo de postulaciones")
    void testGetApplicationCount_Found() {
        request.setStudentApplications(List.of(new MonitorApplication(), new MonitorApplication()));
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        Integer count = service.getApplicationCount(1L);

        assertEquals(2, count);
    }

    @Test
    @DisplayName("getApplicationCount: debe retornar 0 cuando la convocatoria no existe")
    void testGetApplicationCount_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Integer count = service.getApplicationCount(99L);

        assertEquals(0, count);
    }

    // ==================== findAll ====================

    @Test
    @DisplayName("findAll: debe retornar todas las convocatorias")
    void testFindAll() {
        when(monitoringRequestRepository.findAll()).thenReturn(List.of(request));

        List<MonitoringRequest> result = service.findAll();

        assertEquals(1, result.size());
        verify(monitoringRequestRepository).findAll();
    }

    // ==================== findById ====================

    @Test
    @DisplayName("findById: debe retornar la convocatoria con carga lazy")
    void testFindById_Found() {
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        Optional<MonitoringRequest> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(monitoringRequestRepository).findById(1L);
    }

    @Test
    @DisplayName("findById: debe retornar Optional vacío cuando no existe")
    void testFindById_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<MonitoringRequest> result = service.findById(99L);

        assertTrue(result.isEmpty());
    }

    // ==================== save ====================

    @Test
    @DisplayName("save: debe guardar y retornar la convocatoria")
    void testSave() throws Exception {
        when(monitoringRequestRepository.save(request)).thenReturn(request);

        MonitoringRequest result = service.save(request);

        assertEquals(request, result);
        verify(monitoringRequestRepository).save(request);
    }

    // ==================== update ====================

    @Test
    @DisplayName("update: debe actualizar la convocatoria exitosamente")
    void testUpdate_Success() throws Exception {
        request.setId(1L);
        when(monitoringRequestRepository.existsById(1L)).thenReturn(true);
        when(monitoringRequestRepository.save(request)).thenReturn(request);

        MonitoringRequest result = service.update(request);

        assertNotNull(result);
        verify(monitoringRequestRepository).existsById(1L);
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("update: debe lanzar excepción cuando la convocatoria no existe")
    void testUpdate_NotFound() {
        request.setId(99L);
        when(monitoringRequestRepository.existsById(99L)).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> service.update(request));
        assertEquals("No se puede actualizar una convocatoria que no existe", ex.getMessage());
    }

    @Test
    @DisplayName("update: debe lanzar excepción cuando el id es nulo")
    void testUpdate_NullId() {
        request.setId(null);

        Exception ex = assertThrows(Exception.class, () -> service.update(request));
        assertEquals("No se puede actualizar una convocatoria que no existe", ex.getMessage());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete: debe eliminar la convocatoria exitosamente")
    void testDelete_Success() throws Exception {
        request.setStatus(RequestStatus.CANCELADA);
        when(monitorApplicationRepository.countByMonitoringRequest(request)).thenReturn(0L);

        service.delete(request);

        verify(monitoringRequestRepository).delete(request);
    }

    @Test
    @DisplayName("delete: debe lanzar excepción cuando tiene postulaciones y no está cancelada")
    void testDelete_HasApplicationsAndNotCanceled() {
        request.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);
        when(monitorApplicationRepository.countByMonitoringRequest(request)).thenReturn(3L);

        Exception ex = assertThrows(Exception.class, () -> service.delete(request));
        assertEquals("No se puede eliminar una convocatoria con postulaciones", ex.getMessage());
        verify(monitoringRequestRepository, never()).delete(any());
    }

    // ==================== deleteById ====================

    @Test
    @DisplayName("deleteById: debe eliminar por id exitosamente")
    void testDeleteById_Success() throws Exception {
        request.setStatus(RequestStatus.CANCELADA);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(monitorApplicationRepository.countByMonitoringRequest(request)).thenReturn(0L);

        service.deleteById(1L);

        verify(monitoringRequestRepository).findById(1L);
        verify(monitoringRequestRepository).delete(request);
    }

    @Test
    @DisplayName("deleteById: debe lanzar excepción cuando no se encuentra")
    void testDeleteById_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.deleteById(99L));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    // ==================== validate ====================

    @Test
    @DisplayName("validate: debe validar correctamente una entidad válida")
    void testValidate_Success() throws Exception {
        request.setProfessor(professor);
        request.setCourse(course);
        request.setRequestedHours(10);
        request.setJustification("Justificación válida");

        assertDoesNotThrow(() -> service.validate(request));
    }

    @Test
    @DisplayName("validate: debe lanzar excepción cuando el profesor es nulo")
    void testValidate_NullProfessor() {
        request.setProfessor(null);

        Exception ex = assertThrows(Exception.class, () -> service.validate(request));
        assertEquals("El profesor es obligatorio", ex.getMessage());
    }

    @Test
    @DisplayName("validate: debe lanzar excepción cuando el curso es nulo")
    void testValidate_NullCourse() {
        request.setCourse(null);

        Exception ex = assertThrows(Exception.class, () -> service.validate(request));
        assertEquals("El curso es obligatorio", ex.getMessage());
    }

    @Test
    @DisplayName("validate: debe lanzar excepción cuando las horas son nulas o <= 0")
    void testValidate_InvalidHours() {
        request.setRequestedHours(0);

        Exception ex = assertThrows(Exception.class, () -> service.validate(request));
        assertEquals("Las horas solicitadas deben ser mayores a 0", ex.getMessage());
    }

    @Test
    @DisplayName("validate: debe lanzar excepción cuando la justificación es nula o vacía")
    void testValidate_InvalidJustification() {
        request.setJustification("");

        Exception ex = assertThrows(Exception.class, () -> service.validate(request));
        assertEquals("La justificación es obligatoria", ex.getMessage());
    }

    // ==================== count ====================

    @Test
    @DisplayName("count: debe retornar el total de convocatorias")
    void testCount() {
        when(monitoringRequestRepository.count()).thenReturn(5L);

        Long count = service.count();

        assertEquals(5L, count);
        verify(monitoringRequestRepository).count();
    }

    // ==================== findPendingHeadApproval ====================

    @Test
    @DisplayName("findPendingHeadApproval: debe lanzar excepción cuando el jefe no existe")
    void testFindPendingHeadApproval_HeadNotFound() {
        when(departmentHeadRepository.findById("DH001")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.findPendingHeadApproval("DH001"));
        assertEquals("Jefe de departamento no encontrado", ex.getMessage());
    }

    @Test
    @DisplayName("findPendingHeadApproval: debe retornar lista vacía cuando no hay programas asignados")
    void testFindPendingHeadApproval_NoPrograms() throws Exception {
        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");
        when(departmentHeadRepository.findById("DH001")).thenReturn(Optional.of(head));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of());

        List<MonitoringRequest> result = service.findPendingHeadApproval("DH001");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findPendingHeadApproval: debe retornar convocatorias pendientes de aprobación del jefe")
    void testFindPendingHeadApproval_Success() throws Exception {
        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");

        HeadProgram hp = new HeadProgram();
        hp.setId(1);
        hp.setDepartmentHead(head);
        hp.setProgram(program);

        MonitoringRequest pending = new MonitoringRequest();
        pending.setId(10L);
        pending.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        pending.setProgram(program);

        MonitoringRequest other = new MonitoringRequest();
        other.setId(20L);
        other.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);
        other.setProgram(program);

        when(departmentHeadRepository.findById("DH001")).thenReturn(Optional.of(head));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(hp));
        when(monitoringRequestRepository.findAll()).thenReturn(List.of(pending, other));

        List<MonitoringRequest> result = service.findPendingHeadApproval("DH001");

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    @DisplayName("findPendingHeadApproval: debe retornar lista vacía cuando no hay convocatorias pendientes")
    void testFindPendingHeadApproval_NoPending() throws Exception {
        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");

        HeadProgram hp = new HeadProgram();
        hp.setId(1);
        hp.setDepartmentHead(head);
        hp.setProgram(program);

        MonitoringRequest other = new MonitoringRequest();
        other.setId(20L);
        other.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);
        other.setProgram(program);

        when(departmentHeadRepository.findById("DH001")).thenReturn(Optional.of(head));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(hp));
        when(monitoringRequestRepository.findAll()).thenReturn(List.of(other));

        List<MonitoringRequest> result = service.findPendingHeadApproval("DH001");

        assertTrue(result.isEmpty());
    }

    // ==================== approveByHead ====================

    @Test
    @DisplayName("approveByHead: debe aprobar la convocatoria como jefe exitosamente")
    void testApproveByHead_Success() throws Exception {
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);

        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");

        HeadProgram hp = new HeadProgram();
        hp.setId(1);
        hp.setDepartmentHead(head);
        hp.setProgram(program);

        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(hp));

        service.approveByHead(1L, "DH001", "Aprobado, buen curso");

        assertEquals(RequestStatus.CONVOCATORIA_ABIERTA, request.getStatus());
        assertEquals("DH001", request.getApprovedByHead());
        assertEquals("Aprobado, buen curso", request.getHeadComment());
        assertNotNull(request.getHeadApprovalDate());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("approveByHead: debe lanzar excepción cuando la convocatoria no existe")
    void testApproveByHead_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> service.approveByHead(99L, "DH001", "comment"));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("approveByHead: debe lanzar excepción cuando el estado no es PENDIENTE_APROBACION_JEFE")
    void testApproveByHead_WrongStatus() {
        request.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        Exception ex = assertThrows(Exception.class,
                () -> service.approveByHead(1L, "DH001", "comment"));
        assertEquals("La convocatoria no está pendiente de aprobación del jefe", ex.getMessage());
    }

    @Test
    @DisplayName("approveByHead: debe lanzar excepción cuando el jefe no tiene permiso")
    void testApproveByHead_NoPermission() {
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of());

        Exception ex = assertThrows(Exception.class,
                () -> service.approveByHead(1L, "DH001", "comment"));
        assertEquals("El jefe no tiene permiso para aprobar convocatorias de este programa", ex.getMessage());
    }

    // ==================== rejectByHead ====================

    @Test
    @DisplayName("rejectByHead: debe rechazar la convocatoria como jefe exitosamente")
    void testRejectByHead_Success() throws Exception {
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);

        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");

        HeadProgram hp = new HeadProgram();
        hp.setId(1);
        hp.setDepartmentHead(head);
        hp.setProgram(program);

        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(hp));

        service.rejectByHead(1L, "DH001", "No cumple requisitos");

        assertEquals(RequestStatus.RECHAZADA, request.getStatus());
        assertEquals("DH001", request.getApprovedByHead());
        assertEquals("No cumple requisitos", request.getHeadComment());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("rejectByHead: debe lanzar excepción cuando la convocatoria no existe")
    void testRejectByHead_NotFound() {
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> service.rejectByHead(99L, "DH001", "comment"));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("rejectByHead: debe lanzar excepción cuando el estado no es PENDIENTE_APROBACION_JEFE")
    void testRejectByHead_WrongStatus() {
        request.setStatus(RequestStatus.APROBADA);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        Exception ex = assertThrows(Exception.class,
                () -> service.rejectByHead(1L, "DH001", "comment"));
        assertEquals("La convocatoria no está pendiente de aprobación del jefe", ex.getMessage());
    }

    @Test
    @DisplayName("rejectByHead: debe lanzar excepción cuando el jefe no tiene permiso")
    void testRejectByHead_NoPermission() {
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of());

        Exception ex = assertThrows(Exception.class,
                () -> service.rejectByHead(1L, "DH001", "comment"));
        assertEquals("El jefe no tiene permiso para rechazar convocatorias de este programa", ex.getMessage());
    }

    // ==================== modifyAndApproveByHead ====================

    @Test
    @DisplayName("modifyAndApproveByHead: debe modificar y aprobar sin cambios")
    void testModifyAndApproveByHead_NoModifications() throws Exception {
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        request.setRequestedHours(20);

        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");

        HeadProgram hp = new HeadProgram();
        hp.setId(1);
        hp.setDepartmentHead(head);
        hp.setProgram(program);

        MonitoringRequestDTO modDto = new MonitoringRequestDTO();

        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(hp));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());
        when(monitoringRequestRepository.save(any(MonitoringRequest.class))).thenAnswer(i -> i.getArgument(0));

        MonitoringRequest result = service.modifyAndApproveByHead(1L, modDto, "DH001", "Aprobado sin cambios");

        assertEquals(RequestStatus.CONVOCATORIA_ABIERTA, result.getStatus());
        assertEquals(20, result.getRequestedHours());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("modifyAndApproveByHead: debe modificar horas y justificación, luego aprobar")
    void testModifyAndApproveByHead_WithModifications() throws Exception {
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        request.setRequestedHours(20);

        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");

        HeadProgram hp = new HeadProgram();
        hp.setId(1);
        hp.setDepartmentHead(head);
        hp.setProgram(program);

        MonitoringRequestDTO modDto = new MonitoringRequestDTO();
        modDto.setRequestedHours(25);
        modDto.setJustification("Justificación modificada por el jefe");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MARCH, 1, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        modDto.setStartDate(cal.getTime());
        cal.set(2026, Calendar.MAY, 30, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        modDto.setFinishDate(cal.getTime());
        modDto.setRequiredAverageGrade(4.5);
        modDto.setRequiredCourseGrade(4.8);
        modDto.setHourlyRate(16000.0);

        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(hp));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 100)));
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of());
        when(monitoringRequestRepository.save(any(MonitoringRequest.class))).thenAnswer(i -> i.getArgument(0));

        MonitoringRequest result = service.modifyAndApproveByHead(1L, modDto, "DH001", "Aprobado con modificaciones");

        assertEquals(RequestStatus.CONVOCATORIA_ABIERTA, result.getStatus());
        assertEquals(25, result.getRequestedHours());
        assertEquals("Justificación modificada por el jefe", result.getJustification());
        assertEquals(modDto.getStartDate(), result.getStartDate());
        assertEquals(modDto.getFinishDate(), result.getFinishDate());
        assertEquals(4.5, result.getRequiredAverageGrade());
        assertEquals(4.8, result.getRequiredCourseGrade());
        assertEquals(16000.0, result.getHourlyRate());
        verify(monitoringRequestRepository).save(request);
    }

    @Test
    @DisplayName("modifyAndApproveByHead: debe lanzar excepción cuando el presupuesto es insuficiente")
    void testModifyAndApproveByHead_BudgetFail() {
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        request.setRequestedHours(20);

        DepartmentHead head = new DepartmentHead();
        head.setId("DH001");

        HeadProgram hp = new HeadProgram();
        hp.setId(1);
        hp.setDepartmentHead(head);
        hp.setProgram(program);

        MonitoringRequestDTO modDto = new MonitoringRequestDTO();
        modDto.setRequestedHours(200);

        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of(hp));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(departmentBudgetRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(new DepartmentBudget(program, "2026-1", 50)));
        MonitoringRequest used = new MonitoringRequest();
        used.setRequestedHours(40);
        used.setStatus(RequestStatus.APROBADA);
        when(monitoringRequestRepository.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(List.of(used));

        Exception ex = assertThrows(Exception.class,
                () -> service.modifyAndApproveByHead(1L, modDto, "DH001", "comment"));
        assertEquals("Las horas modificadas exceden el presupuesto disponible", ex.getMessage());
        verify(monitoringRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("modifyAndApproveByHead: debe lanzar excepción cuando la convocatoria no existe")
    void testModifyAndApproveByHead_NotFound() {
        MonitoringRequestDTO modDto = new MonitoringRequestDTO();
        when(monitoringRequestRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> service.modifyAndApproveByHead(99L, modDto, "DH001", "comment"));
        assertEquals("Convocatoria no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("modifyAndApproveByHead: debe lanzar excepción cuando el estado no es PENDIENTE_APROBACION_JEFE")
    void testModifyAndApproveByHead_WrongStatus() {
        request.setStatus(RequestStatus.APROBADA);
        MonitoringRequestDTO modDto = new MonitoringRequestDTO();
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        Exception ex = assertThrows(Exception.class,
                () -> service.modifyAndApproveByHead(1L, modDto, "DH001", "comment"));
        assertEquals("La convocatoria no está pendiente de aprobación del jefe", ex.getMessage());
    }

    @Test
    @DisplayName("modifyAndApproveByHead: debe lanzar excepción cuando el jefe no tiene permiso")
    void testModifyAndApproveByHead_NoPermission() {
        request.setStatus(RequestStatus.PENDIENTE_APROBACION_JEFE);
        MonitoringRequestDTO modDto = new MonitoringRequestDTO();
        when(monitoringRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(headProgramRepository.findByDepartmentHeadId("DH001")).thenReturn(List.of());

        Exception ex = assertThrows(Exception.class,
                () -> service.modifyAndApproveByHead(1L, modDto, "DH001", "comment"));
        assertEquals("El jefe no tiene permiso para modificar convocatorias de este programa", ex.getMessage());
    }
}
