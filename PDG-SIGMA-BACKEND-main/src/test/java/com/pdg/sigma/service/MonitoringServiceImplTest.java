package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceImplTest {

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private HeadProgramRepository headProgramRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private DepartmentHeadRepository departmentHeadRepository;

    @Mock
    private CourseProfessorRepository courseProfessorRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private DepartmentBudgetRepository departmentBudgetRepository;

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @Mock
    private MonitoringRequestRepository monitoringRequestRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MonitoringServiceImpl monitoringService;

    private School sampleSchool;
    private Program sampleProgram;
    private Course sampleCourse;
    private Professor sampleProfessor;
    private Monitor sampleMonitor;
    private Monitoring sampleMonitoring;
    private Date pastDate;
    private Date futureDate;

    @BeforeEach
    void setUp() {
        sampleSchool = new School();
        sampleSchool.setId(1L);
        sampleSchool.setName("Facultad de Ingeniería");

        sampleProgram = new Program();
        sampleProgram.setId(1L);
        sampleProgram.setName("Ingeniería de Sistemas");

        sampleCourse = new Course();
        sampleCourse.setId(1L);
        sampleCourse.setName("Programación I");
        sampleCourse.setProgram(sampleProgram);

        sampleProfessor = new Professor();
        sampleProfessor.setId("P001");
        sampleProfessor.setName("Dr. García");

        sampleMonitor = new Monitor();
        sampleMonitor.setCode("M001");
        sampleMonitor.setName("Juan");
        sampleMonitor.setLastName("Pérez");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        pastDate = cal.getTime();

        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 30);
        futureDate = cal.getTime();

        sampleMonitoring = new Monitoring();
        sampleMonitoring.setId(1L);
        sampleMonitoring.setSchool(sampleSchool);
        sampleMonitoring.setProgram(sampleProgram);
        sampleMonitoring.setCourse(sampleCourse);
        sampleMonitoring.setProfessor(sampleProfessor);
        sampleMonitoring.setSemester("2026-1");
        sampleMonitoring.setStart(pastDate);
        sampleMonitoring.setFinish(futureDate);
        sampleMonitoring.setEstimatedHours(10);
        sampleMonitoring.setHourlyRate(50000.0);
    }

    // ==================== findAll ====================

    @Test
    void findAll_filtersApprovedMonitorings() {
        Monitoring m2 = new Monitoring();
        m2.setId(2L);
        m2.setStart(pastDate);
        m2.setFinish(futureDate);

        when(monitoringRepository.findAll()).thenReturn(List.of(sampleMonitoring, m2));
        when(monitoringMonitorRepository.findByMonitoring(sampleMonitoring)).thenReturn(List.of());
        when(monitoringMonitorRepository.findByMonitoring(m2)).thenReturn(List.of());

        List<Monitoring> result = monitoringService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findAll_excludesApprovedMonitorings() {
        MonitoringMonitor mm = new MonitoringMonitor();
        mm.setEstadoSeleccion("aprobado");
        when(monitoringRepository.findAll()).thenReturn(List.of(sampleMonitoring));
        when(monitoringMonitorRepository.findByMonitoring(sampleMonitoring)).thenReturn(List.of(mm));

        List<Monitoring> result = monitoringService.findAll();

        assertTrue(result.isEmpty());
    }

    // ==================== findAllByProfessor ====================

    @Test
    void findAllByProfessor_professorFound_returnsList() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of(sampleMonitoring));

        List<Monitoring> result = monitoringService.findAllByProfessor("P001");

        assertEquals(1, result.size());
    }

    @Test
    void findAllByProfessor_professorNotFound_returnsNull() {
        when(professorRepository.findById("INVALID")).thenReturn(Optional.empty());

        List<Monitoring> result = monitoringService.findAllByProfessor("INVALID");

        assertNull(result);
    }

    // ==================== findById ====================

    @Test
    void findById_found_returnsMonitoring() {
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(sampleMonitoring));

        Optional<Monitoring> result = monitoringService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(monitoringRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Monitoring> result = monitoringService.findById(99L);

        assertFalse(result.isPresent());
    }

    // ==================== save (MonitoringDTO) ====================

    @Test
    void save_withValidData_createsMonitoring() throws Exception {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setSchoolName("Facultad de Ingeniería");
        dto.setProgramName("Ingeniería de Sistemas");
        dto.setCourseName("Programación I");
        dto.setProfessorId("P001");
        dto.setSemester("2026-1");
        dto.setStart(pastDate);
        dto.setFinish(futureDate);
        dto.setEstimatedHours(10);
        dto.setHourlyRate(50000.0);

        when(schoolRepository.findByNameIgnoreCase("Facultad de Ingeniería")).thenReturn(Optional.of(sampleSchool));
        when(programRepository.findByNameIgnoreCaseAndSchool("Ingeniería de Sistemas", sampleSchool)).thenReturn(Optional.of(sampleProgram));
        when(courseRepository.findByNameIgnoreCaseAndProgram("Programación I", sampleProgram)).thenReturn(Optional.of(sampleCourse));
        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessorAndCourseAndSemester(sampleProfessor, sampleCourse, "2026-1")).thenReturn(Optional.empty());

        Monitoring result = monitoringService.save(dto);

        assertNotNull(result);
        assertEquals("2026-1", result.getSemester());
        verify(monitoringRepository).save(any(Monitoring.class));
    }

    @Test
    void save_nullDTO_throwsException() {
        Exception e = assertThrows(Exception.class, () -> monitoringService.save((MonitoringDTO) null));
        assertTrue(e.getMessage().contains("invalido"));
    }

    @Test
    void save_nullSchoolName_throwsException() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setSchoolName(null);
        dto.setProgramName("Ingeniería");
        dto.setCourseName("Programación");
        dto.setProfessorId("P001");
        dto.setSemester("2026-1");

        Exception e = assertThrows(Exception.class, () -> monitoringService.save(dto));
        assertTrue(e.getMessage().contains("facultad"));
    }

    @Test
    void save_nullSemester_throwsException() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setSchoolName("Facultad");
        dto.setProgramName("Ingeniería");
        dto.setCourseName("Programación");
        dto.setProfessorId("P001");

        Exception e = assertThrows(Exception.class, () -> monitoringService.save(dto));
        assertTrue(e.getMessage().contains("periodo"));
    }

    @Test
    void save_nullStartOrFinish_throwsException() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setSchoolName("Facultad");
        dto.setProgramName("Ingeniería");
        dto.setCourseName("Programación");
        dto.setProfessorId("P001");
        dto.setSemester("2026-1");

        Exception e = assertThrows(Exception.class, () -> monitoringService.save(dto));
        assertTrue(e.getMessage().contains("fechas"));
    }

    @Test
    void save_duplicateMonitoring_throwsException() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setSchoolName("Facultad de Ingeniería");
        dto.setProgramName("Ingeniería de Sistemas");
        dto.setCourseName("Programación I");
        dto.setProfessorId("P001");
        dto.setSemester("2026-1");
        dto.setStart(pastDate);
        dto.setFinish(futureDate);

        when(schoolRepository.findByNameIgnoreCase("Facultad de Ingeniería")).thenReturn(Optional.of(sampleSchool));
        when(programRepository.findByNameIgnoreCaseAndSchool("Ingeniería de Sistemas", sampleSchool)).thenReturn(Optional.of(sampleProgram));
        when(courseRepository.findByNameIgnoreCaseAndProgram("Programación I", sampleProgram)).thenReturn(Optional.of(sampleCourse));
        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessorAndCourseAndSemester(sampleProfessor, sampleCourse, "2026-1")).thenReturn(Optional.of(sampleMonitoring));

        Exception e = assertThrows(Exception.class, () -> monitoringService.save(dto));
        assertTrue(e.getMessage().toLowerCase().contains("ya existe"));
    }

    @Test
    void save_schoolNotFound_throwsException() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setSchoolName("Facultad Inexistente");
        dto.setProgramName("Ingeniería");
        dto.setCourseName("Programación");
        dto.setProfessorId("P001");
        dto.setSemester("2026-1");
        dto.setStart(pastDate);
        dto.setFinish(futureDate);

        when(schoolRepository.findByNameIgnoreCase("Facultad Inexistente")).thenReturn(Optional.empty());
        when(schoolRepository.findAll()).thenReturn(List.of());

        Exception e = assertThrows(Exception.class, () -> monitoringService.save(dto));
        assertTrue(e.getMessage().contains("facultad"));
    }

    // ==================== updateMonitoringBudget ====================

    @Test
    void updateMonitoringBudget_validData_updatesBudget() throws Exception {
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(sampleMonitoring));
        when(monitoringRepository.save(any(Monitoring.class))).thenReturn(sampleMonitoring);

        Monitoring result = monitoringService.updateMonitoringBudget(1L, 20, 60000.0);

        assertNotNull(result);
        verify(monitoringRepository).save(sampleMonitoring);
    }

    @Test
    void updateMonitoringBudget_monitoringNotFound_throwsException() {
        when(monitoringRepository.findById(99L)).thenReturn(Optional.empty());

        Exception e = assertThrows(Exception.class, () -> monitoringService.updateMonitoringBudget(99L, 10, 50000.0));
        assertTrue(e.getMessage().contains("no encontrada"));
    }

    @Test
    void updateMonitoringBudget_negativeHours_throwsException() {
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(sampleMonitoring));

        Exception e = assertThrows(Exception.class, () -> monitoringService.updateMonitoringBudget(1L, -5, 50000.0));
        assertTrue(e.getMessage().contains("negativa"));
    }

    @Test
    void updateMonitoringBudget_negativeRate_throwsException() {
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(sampleMonitoring));

        Exception e = assertThrows(Exception.class, () -> monitoringService.updateMonitoringBudget(1L, 10, -1.0));
        assertTrue(e.getMessage().contains("negativo"));
    }

    @Test
    void updateMonitoringBudget_exceedsBudget_throwsException() {
        DepartmentBudget budget = new DepartmentBudget();
        budget.setTotalHours(10);

        Monitoring otherMonitoring = new Monitoring();
        otherMonitoring.setId(2L);
        otherMonitoring.setSemester("2026-1");
        otherMonitoring.setEstimatedHours(8);

        sampleMonitoring.setSemester("2026-1");
        sampleMonitoring.setProgram(sampleProgram);
        sampleMonitoring.setEstimatedHours(0);

        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(sampleMonitoring));
        when(departmentBudgetRepository.findByProgramAndSemester(sampleProgram, "2026-1")).thenReturn(Optional.of(budget));
        when(monitoringRepository.findByProgram(sampleProgram)).thenReturn(List.of(sampleMonitoring, otherMonitoring));

        Exception e = assertThrows(Exception.class, () -> monitoringService.updateMonitoringBudget(1L, 5, 50000.0));
        assertTrue(e.getMessage().contains("Disponibles"));
    }

    // ==================== deleteMonitoring ====================

    @Test
    void deleteMonitoring_foundAndNoRelations_returnsTrue() {
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(sampleMonitoring));
        when(monitoringMonitorRepository.findByMonitoring(sampleMonitoring)).thenReturn(List.of());

        boolean result = monitoringService.deleteMonitoring(1L);

        assertTrue(result);
        verify(monitoringRepository).delete(sampleMonitoring);
    }

    @Test
    void deleteMonitoring_foundWithRelations_returnsFalse() {
        MonitoringMonitor mm = new MonitoringMonitor();
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(sampleMonitoring));
        when(monitoringMonitorRepository.findByMonitoring(sampleMonitoring)).thenReturn(List.of(mm));

        boolean result = monitoringService.deleteMonitoring(1L);

        assertFalse(result);
        verify(monitoringRepository, never()).delete(any());
    }

    @Test
    void deleteMonitoring_notFound_returnsFalse() {
        when(monitoringRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = monitoringService.deleteMonitoring(99L);

        assertFalse(result);
    }

    // ==================== findPendingApproval ====================

    @Test
    void findPendingApproval_returnsMonitoringsRequiringApproval() {
        Monitoring pending = new Monitoring();
        pending.setSemester("2026-1");
        when(monitoringRepository.findAll()).thenReturn(List.of(pending));

        List<Monitoring> result = monitoringService.findPendingApproval();

        assertNotNull(result);
    }

    // ==================== approveMonitoring ====================

    @Test
    void approveMonitoring_validData_approves() throws Exception {
        Monitoring monitoring = mock(Monitoring.class);
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(monitoring));
        when(monitoring.isFromNewFlow()).thenReturn(true);
        when(monitoring.requiresApproval()).thenReturn(true);

        monitoringService.approveMonitoring(1L, "H001", "Approved");

        verify(monitoring).approve("H001", "Approved");
        verify(monitoringRepository).save(monitoring);
    }

    @Test
    void approveMonitoring_notFound_throwsException() {
        when(monitoringRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> monitoringService.approveMonitoring(99L, "H001", "Comment"));
    }

    @Test
    void approveMonitoring_notNewFlow_throwsException() {
        Monitoring monitoring = mock(Monitoring.class);
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(monitoring));
        when(monitoring.isFromNewFlow()).thenReturn(false);

        Exception e = assertThrows(Exception.class, () -> monitoringService.approveMonitoring(1L, "H001", "Comment"));
        assertTrue(e.getMessage().contains("nuevo flujo"));
    }

    @Test
    void approveMonitoring_notPendingApproval_throwsException() {
        Monitoring monitoring = mock(Monitoring.class);
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(monitoring));
        when(monitoring.isFromNewFlow()).thenReturn(true);
        when(monitoring.requiresApproval()).thenReturn(false);
            when(monitoring.getApprovalStatus()).thenReturn(MonitoringApprovalStatus.APROBADA);

        Exception e = assertThrows(Exception.class, () -> monitoringService.approveMonitoring(1L, "H001", "Comment"));
        assertTrue(e.getMessage().contains("pendiente"));
    }

    // ==================== rejectMonitoring ====================

    @Test
    void rejectMonitoring_validData_rejects() throws Exception {
        Monitoring monitoring = mock(Monitoring.class);
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(monitoring));
        when(monitoring.isFromNewFlow()).thenReturn(true);
        when(monitoring.requiresApproval()).thenReturn(true);

        monitoringService.rejectMonitoring(1L, "H001", "Rejected");

        verify(monitoring).reject("H001", "Rejected");
        verify(monitoringRepository).save(monitoring);
    }

    @Test
    void rejectMonitoring_notFound_throwsException() {
        when(monitoringRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> monitoringService.rejectMonitoring(99L, "H001", "Comment"));
    }

    @Test
    void rejectMonitoring_notNewFlow_throwsException() {
        Monitoring monitoring = mock(Monitoring.class);
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(monitoring));
        when(monitoring.isFromNewFlow()).thenReturn(false);

        Exception e = assertThrows(Exception.class, () -> monitoringService.rejectMonitoring(1L, "H001", "Comment"));
        assertTrue(e.getMessage().contains("nuevo flujo"));
    }

    // ==================== findMonitoringsByProfessorWithAssignedMonitors ====================

    @Test
    void findMonitoringsByProfessorWithAssignedMonitors_returnsList() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of(sampleMonitoring));
        when(monitoringRepository.findMonitoringsByProfessorAndHavingSelectedMonitors("P001"))
                .thenReturn(List.of(sampleMonitoring));

        List<Monitoring> result = monitoringService.findMonitoringsByProfessorWithAssignedMonitors("P001");

        assertEquals(1, result.size());
    }

    // ==================== findMonitoringsByAssignedMonitor ====================

    @Test
    void findMonitoringsByAssignedMonitor_returnsList() {
        when(monitoringRepository.findMonitoringsDirectlyAssignedToMonitorWithStatusSelected("M001"))
                .thenReturn(List.of(sampleMonitoring));

        List<Monitoring> result = monitoringService.findMonitoringsByAssignedMonitor("M001");

        assertEquals(1, result.size());
    }

    // ==================== getByProfessor ====================

    @Test
    void getByProfessor_professorFoundWithMonitorings_returnsDTOs() throws Exception {
        MonitoringMonitor mm = new MonitoringMonitor();
        mm.setEstadoSeleccion("seleccionado");
        mm.setMonitor(sampleMonitor);
        mm.setMonitoring(sampleMonitoring);

        sampleMonitoring.setCourse(sampleCourse);
        sampleCourse.setProgram(sampleProgram);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of(sampleMonitoring));
        when(monitoringMonitorRepository.findByMonitoring(sampleMonitoring)).thenReturn(List.of(mm));

        List<MonitoringDTO> result = monitoringService.getByProfessor("P001");

        assertEquals(1, result.size());
    }

    @Test
    void getByProfessor_professorNotFound_throwsException() {
        when(professorRepository.findById("INVALID")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> monitoringService.getByProfessor("INVALID"));
    }

    @Test
    void getByProfessor_noMonitorings_throwsException() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of());

        Exception e = assertThrows(Exception.class, () -> monitoringService.getByProfessor("P001"));
        assertTrue(e.getMessage().contains("No tiene monitorias"));
    }

    // ==================== getByMonitor ====================

    @Test
    void getByMonitor_monitorFoundWithMonitorings_returnsDTOs() throws Exception {
        MonitoringMonitor mm = new MonitoringMonitor();
        mm.setMonitor(sampleMonitor);
        mm.setMonitoring(sampleMonitoring);
        sampleMonitoring.setCourse(sampleCourse);
        sampleMonitoring.setProfessor(sampleProfessor);

        when(monitorRepository.findByIdMonitor("M001")).thenReturn(Optional.of(sampleMonitor));
        when(monitoringMonitorRepository.findByMonitor(sampleMonitor)).thenReturn(List.of(mm));

        List<MonitoringDTO> result = monitoringService.getByMonitor("M001");

        assertEquals(1, result.size());
    }

    @Test
    void getByMonitor_monitorNotFound_throwsException() {
        when(monitorRepository.findByIdMonitor("INVALID")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> monitoringService.getByMonitor("INVALID"));
    }

    // ==================== getProfessorReport ====================

    @Test
    void getProfessorReport_professorFoundWithData_returnsReport() throws Exception {
        Activity activity = new Activity();
        activity.setMonitoring(sampleMonitoring);
        activity.setState(StateActivity.COMPLETADO);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of(sampleMonitoring));
        when(activityRepository.findByProfessorAndRoleResponsable(sampleProfessor, "P")).thenReturn(List.of(activity));
        when(activityRepository.findByProfessorAndRoleCreator(sampleProfessor, "P")).thenReturn(List.of());

        List<ReportDTO> result = monitoringService.getProfessorReport("P001");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getCompleted());
    }

    @Test
    void getProfessorReport_professorNotFound_throwsException() {
        when(professorRepository.findById("INVALID")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> monitoringService.getProfessorReport("INVALID"));
    }

    @Test
    void getProfessorReport_noMonitorings_throwsException() {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of());

        Exception e = assertThrows(Exception.class, () -> monitoringService.getProfessorReport("P001"));
        assertTrue(e.getMessage().contains("No hay monitor"));
    }

    // ==================== getMonthlyAttendanceReport ====================

    @Test
    void getMonthlyAttendanceReport_professorFoundWithData_returnsReport() throws Exception {
        Activity activity = new Activity();
        activity.setMonitoring(sampleMonitoring);
        activity.setDelivey(new Date());

        Attendance attendance = new Attendance();
        attendance.setActivity(activity);
        Student student = new Student();
        student.setName("Juan Pérez");
        attendance.setStudent(student);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of(sampleMonitoring));
        when(activityRepository.findByMonitoring(sampleMonitoring)).thenReturn(List.of(activity));
        when(attendanceRepository.findByActivityIn(anyList())).thenReturn(List.of(attendance));

        List<Map<String, Object>> result = monitoringService.getMonthlyAttendanceReport("P001", Optional.empty());

        assertEquals(1, result.size());
    }

    @Test
    void getMonthlyAttendanceReport_professorNotFound_throwsException() {
        when(professorRepository.findById("INVALID")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> monitoringService.getMonthlyAttendanceReport("INVALID", Optional.empty()));
    }

    @Test
    void getMonthlyAttendanceReport_noMonitorings_returnsEmptyList() throws Exception {
        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of());

        List<Map<String, Object>> result = monitoringService.getMonthlyAttendanceReport("P001", Optional.empty());

        assertTrue(result.isEmpty());
    }

    // ==================== findBySchool ====================

    @Test
    void findBySchool_withProgramAndActive_returnsFiltered() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setProgramName("Facultad de Ingeniería");
        dto.setCourseName("Activo");

        when(schoolRepository.findByName("Facultad de Ingeniería")).thenReturn(Optional.of(sampleSchool));
        when(monitoringRepository.findBySchool(sampleSchool)).thenReturn(List.of(sampleMonitoring));

        List<Monitoring> result = monitoringService.findBySchool(dto);

        assertEquals(1, result.size());
    }

    @Test
    void findBySchool_withProgramAndInactive_returnsFiltered() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setProgramName("Facultad de Ingeniería");
        dto.setCourseName("Inactivo");

        sampleMonitoring.setStart(futureDate);
        sampleMonitoring.setFinish(futureDate);

        when(schoolRepository.findByName("Facultad de Ingeniería")).thenReturn(Optional.of(sampleSchool));
        when(monitoringRepository.findBySchool(sampleSchool)).thenReturn(List.of(sampleMonitoring));

        List<Monitoring> result = monitoringService.findBySchool(dto);

        assertEquals(1, result.size());
    }

    // ==================== findByProgram ====================

    @Test
    void findByProgram_withActiveStatus_returnsFiltered() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setProgramName("Ingeniería de Sistemas");
        dto.setCourseName("Activo");

        when(programRepository.findByName("Ingeniería de Sistemas")).thenReturn(Optional.of(sampleProgram));
        when(monitoringRepository.findByProgram(sampleProgram)).thenReturn(List.of(sampleMonitoring));

        List<Monitoring> result = monitoringService.findByProgram(dto);

        assertEquals(1, result.size());
    }

    // ==================== getByHeadDepartment ====================

    @Test
    void getByHeadDepartment_withData_returnsDTOs() throws Exception {
        HeadProgram hp = new HeadProgram();
        hp.setProgram(sampleProgram);

        when(headProgramRepository.findByDepartmentHeadId("H001")).thenReturn(List.of(hp));
        when(courseRepository.findByProgram(sampleProgram)).thenReturn(List.of(sampleCourse));
        when(monitoringRepository.findByCourse(sampleCourse)).thenReturn(Optional.of(sampleMonitoring));
        when(monitoringMonitorRepository.findByMonitoring(sampleMonitoring)).thenReturn(List.of());

        List<MonitoringDTO> result = monitoringService.getByHeadDepartment("H001");

        assertEquals(1, result.size());
    }

    @Test
    void getByHeadDepartment_noHeadPrograms_throwsException() {
        when(headProgramRepository.findByDepartmentHeadId("INVALID")).thenReturn(List.of());

        Exception e = assertThrows(Exception.class, () -> monitoringService.getByHeadDepartment("INVALID"));
        assertTrue(e.getMessage().contains("jefe"));
    }

    @Test
    void getByHeadDepartment_noMonitorings_throwsException() {
        HeadProgram hp = new HeadProgram();
        hp.setProgram(sampleProgram);

        when(headProgramRepository.findByDepartmentHeadId("H001")).thenReturn(List.of(hp));
        when(courseRepository.findByProgram(sampleProgram)).thenReturn(List.of(sampleCourse));
        when(monitoringRepository.findByCourse(sampleCourse)).thenReturn(Optional.empty());

        Exception e = assertThrows(Exception.class, () -> monitoringService.getByHeadDepartment("H001"));
        assertTrue(e.getMessage().contains("No hay monitorias"));
    }

    // ==================== findByCourse ====================

    @Test
    void findByCourse_withActiveStatus_returnsFiltered() {
        MonitoringDTO dto = new MonitoringDTO();
        dto.setProgramName("Programación I");
        dto.setCourseName("Activo");

        when(courseRepository.findByName("Programación I")).thenReturn(Optional.of(sampleCourse));
        when(monitoringRepository.findByCourse(sampleCourse)).thenReturn(Optional.of(sampleMonitoring));

        List<Monitoring> result = monitoringService.findByCourse(dto);

        assertEquals(1, result.size());
    }

    // ==================== getCategoryReport ====================

    @Test
    void getCategoryReport_professorFound_returnsReport() throws Exception {
        Activity activity = new Activity();
        activity.setMonitoring(sampleMonitoring);
        activity.setCategory("Categoría A");
        activity.setId(1);

        when(professorRepository.findById("P001")).thenReturn(Optional.of(sampleProfessor));
        when(monitoringRepository.findByProfessor(sampleProfessor)).thenReturn(List.of(sampleMonitoring));
        when(activityRepository.findByMonitoring(sampleMonitoring)).thenReturn(List.of(activity));

        Map<String, Object> result = monitoringService.getCategoryReport("P001", Optional.empty());

        assertNotNull(result);
        assertTrue(result.containsKey("detalle_por_curso"));
        assertTrue(result.containsKey("totales_por_categoria"));
    }

    @Test
    void getCategoryReport_professorNotFound_throwsException() {
        when(professorRepository.findById("INVALID")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> monitoringService.getCategoryReport("INVALID", Optional.empty()));
    }
}
