package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.SimonMonitoringDTO;
import com.pdg.sigma.dto.SimonMonitoringRowDTO;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.SimonFileGenerationRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimonFileServiceImplTest {

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private SimonFileGenerationRepository simonFileGenerationRepository;

    @InjectMocks
    private SimonFileServiceImpl simonFileService;

    private MonitoringMonitor approvedRelation;
    private MonitoringMonitor relationWithoutMonitor;
    private MonitoringMonitor relationWithoutMonitoring;

    @BeforeEach
    void setUp() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        Professor professor = new Professor();
        professor.setId("P001");
        professor.setName("Dr. Juan Pérez");

        Course course = new Course();
        course.setId(101L);
        course.setName("Programación I");

        Monitor monitor = new Monitor();
        monitor.setCode("M001");
        monitor.setIdMonitor("12345678");
        monitor.setName("Carlos");
        monitor.setLastName("Pérez");
        monitor.setEmail("carlos@test.com");

        School school = new School();
        school.setName("Facultad de Ingeniería");

        Program program = new Program();
        program.setName("Ingeniería de Sistemas");

        Monitoring monitoring = new Monitoring();
        monitoring.setSemester("2025-1");
        monitoring.setStart(sdf.parse("01/02/2025"));
        monitoring.setFinish(sdf.parse("30/06/2025"));
        monitoring.setEstimatedHours(40);
        monitoring.setCourse(course);
        monitoring.setProfessor(professor);
        monitoring.setSchool(school);
        monitoring.setProgram(program);

        approvedRelation = new MonitoringMonitor();
        approvedRelation.setMonitor(monitor);
        approvedRelation.setMonitoring(monitoring);
        approvedRelation.setEstadoSeleccion("aprobado");

        relationWithoutMonitor = new MonitoringMonitor();
        relationWithoutMonitor.setMonitor(null);
        relationWithoutMonitor.setMonitoring(monitoring);

        relationWithoutMonitoring = new MonitoringMonitor();
        relationWithoutMonitoring.setMonitor(monitor);
        relationWithoutMonitoring.setMonitoring(null);
    }

    // ---- getApprovedMonitoringsForSimon (with semester Optional) ----

    @Test
    void getApprovedMonitoringsForSimon_withSemester_returnsFiltered() {
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(approvedRelation));

        List<SimonMonitoringRowDTO> result = simonFileService.getApprovedMonitoringsForSimon(Optional.of("2025-1"));

        assertEquals(1, result.size());
        assertEquals("Carlos", result.get(0).getNombre());
        assertEquals("Pérez", result.get(0).getApellido());
        assertEquals("M001", result.get(0).getCodigoEstudiante());
        assertEquals("carlos@test.com", result.get(0).getEmail());
        assertEquals("Programación I", result.get(0).getNombreCurso());
        assertEquals("Dr. Juan Pérez", result.get(0).getProfesorSolicita());
        assertEquals("01/02/2025", result.get(0).getFechaInicio());
        assertEquals("30/06/2025", result.get(0).getFechaFin());
        assertEquals(40, result.get(0).getTotalHoras());
        assertEquals("2025-1", result.get(0).getSemestre());
        assertEquals("Facultad de Ingeniería", result.get(0).getFacultad());
        assertEquals("Ingeniería de Sistemas", result.get(0).getPrograma());
        assertEquals("101", result.get(0).getCodigoCurso());
        assertEquals("P001", result.get(0).getIdProfesor());
        assertEquals("12345678", result.get(0).getCodigoMonitor());
    }

    @Test
    void getApprovedMonitoringsForSimon_semesterMismatch_skips() {
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(approvedRelation));

        List<SimonMonitoringRowDTO> result = simonFileService.getApprovedMonitoringsForSimon(Optional.of("2024-2"));

        assertTrue(result.isEmpty());
    }

    @Test
    void getApprovedMonitoringsForSimon_withoutSemester_returnsAll() {
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(approvedRelation));

        List<SimonMonitoringRowDTO> result = simonFileService.getApprovedMonitoringsForSimon(Optional.empty());

        assertEquals(1, result.size());
    }

    @Test
    void getApprovedMonitoringsForSimon_skipsNullMonitorOrMonitoring() {
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(relationWithoutMonitor, relationWithoutMonitoring, approvedRelation));

        List<SimonMonitoringRowDTO> result = simonFileService.getApprovedMonitoringsForSimon(Optional.empty());

        assertEquals(1, result.size());
    }

    @Test
    void getApprovedMonitoringsForSimon_nullFields_handlesGracefully() {
        MonitoringMonitor rel = new MonitoringMonitor();
        Monitoring m = new Monitoring();
        Monitor mon = new Monitor();
        rel.setMonitoring(m);
        rel.setMonitor(mon);
        rel.setEstadoSeleccion("aprobado");

        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(rel));

        List<SimonMonitoringRowDTO> result = simonFileService.getApprovedMonitoringsForSimon(Optional.empty());

        assertEquals(1, result.size());
        assertNull(result.get(0).getNombre());
    }

    // ---- buildSimonWorkbook ----

    @Test
    void buildSimonWorkbook_withRows_createsWorkbook() throws Exception {
        SimonMonitoringRowDTO row = new SimonMonitoringRowDTO();
        row.setNombre("Carlos");
        row.setApellido("Pérez");
        row.setCodigoEstudiante("M001");
        row.setEmail("carlos@test.com");
        row.setNombreCurso("Programación I");
        row.setProfesorSolicita("Dr. Juan");
        row.setFechaInicio("01/02/2025");
        row.setFechaFin("30/06/2025");
        row.setTotalHoras(40);
        row.setSemestre("2025-1");
        row.setFacultad("Ingeniería");
        row.setPrograma("Sistemas");
        row.setCodigoCurso("101");
        row.setIdProfesor("P001");
        row.setCodigoMonitor("12345678");

        ByteArrayOutputStream out = simonFileService.buildSimonWorkbook(List.of(row));

        assertNotNull(out);
        assertTrue(out.size() > 0);

        Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, wb.getNumberOfSheets());
        assertEquals(2, wb.getSheetAt(0).getPhysicalNumberOfRows()); // header + data
        assertEquals("Carlos", wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
        assertEquals(40.0, wb.getSheetAt(0).getRow(1).getCell(8).getNumericCellValue(), 0.001);
        wb.close();
    }

    @Test
    void buildSimonWorkbook_nullValues_replacesWithEmpty() throws Exception {
        SimonMonitoringRowDTO row = new SimonMonitoringRowDTO();

        ByteArrayOutputStream out = simonFileService.buildSimonWorkbook(List.of(row));

        Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(out.toByteArray()));
        assertEquals("", wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
        wb.close();
    }

    @Test
    void buildSimonWorkbook_emptyRows_createsHeaderOnly() throws Exception {
        ByteArrayOutputStream out = simonFileService.buildSimonWorkbook(List.of());

        Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, wb.getSheetAt(0).getPhysicalNumberOfRows()); // header only
        wb.close();
    }

    // ---- getGenerationHistory ----

    @Test
    void getGenerationHistory_withSemester_returnsFiltered() {
        SimonFileGeneration record = new SimonFileGeneration();
        record.setSemester("2025-1");
        when(simonFileGenerationRepository.findBySemesterOrderByGeneratedAtDesc("2025-1"))
                .thenReturn(List.of(record));

        List<SimonFileGeneration> result = simonFileService.getGenerationHistory(Optional.of("2025-1"));

        assertEquals(1, result.size());
        verify(simonFileGenerationRepository).findBySemesterOrderByGeneratedAtDesc("2025-1");
    }

    @Test
    void getGenerationHistory_withoutSemester_returnsAll() {
        SimonFileGeneration record = new SimonFileGeneration();
        when(simonFileGenerationRepository.findAllByOrderByGeneratedAtDesc())
                .thenReturn(List.of(record));

        List<SimonFileGeneration> result = simonFileService.getGenerationHistory(Optional.empty());

        assertEquals(1, result.size());
    }

    // ---- auditGeneration ----

    @Test
    void auditGeneration_savesRecord() {
        SimonFileGeneration saved = new SimonFileGeneration();
        saved.setId(1L);
        when(simonFileGenerationRepository.save(any(SimonFileGeneration.class))).thenReturn(saved);

        SimonFileGeneration result = simonFileService.auditGeneration("admin", "2025-1", 5, "test.xlsx");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(simonFileGenerationRepository).save(any(SimonFileGeneration.class));
    }

    // ---- getApprovedMonitoringsForSimon (legacy, no params) ----

    @Test
    void getApprovedMonitoringsForSimon_legacy_returnsList() {
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(approvedRelation));

        List<SimonMonitoringDTO> result = simonFileService.getApprovedMonitoringsForSimon();

        assertEquals(1, result.size());
        assertEquals("Carlos", result.get(0).getNombre());
        assertEquals("PRE", result.get(0).getEstudianteTipo());
        assertEquals("CA021202", result.get(0).getCenco());
        assertEquals("12345678", result.get(0).getCedula());
        assertEquals("TIC - 101", result.get(0).getCodigoCurso());
        assertEquals("Programación I", result.get(0).getNombreCurso());
        assertEquals("01/02/2025", result.get(0).getFechaInicio());
        assertEquals("30/06/2025", result.get(0).getFechaFin());
        assertEquals(40, result.get(0).getTotalHoras());
        assertEquals("Dr. Juan Pérez", result.get(0).getProfesorSolicita());
    }

    @Test
    void getApprovedMonitoringsForSimon_legacy_skipsNullMonitorOrMonitoring() {
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(relationWithoutMonitor, relationWithoutMonitoring));

        List<SimonMonitoringDTO> result = simonFileService.getApprovedMonitoringsForSimon();

        assertTrue(result.isEmpty());
    }

    @Test
    void getApprovedMonitoringsForSimon_legacy_calculatesWeeks() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Monitoring m = new Monitoring();
        m.setStart(sdf.parse("01/02/2025"));
        m.setFinish(sdf.parse("30/06/2025"));
        m.setEstimatedHours(30);
        Monitor mon = new Monitor();
        mon.setName("Carlos");
        mon.setCode("M001");
        mon.setIdMonitor("12345678");
        MonitoringMonitor rel = new MonitoringMonitor();
        rel.setMonitoring(m);
        rel.setMonitor(mon);
        rel.setEstadoSeleccion("aprobado");

        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(rel));

        List<SimonMonitoringDTO> result = simonFileService.getApprovedMonitoringsForSimon();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getTotalSemanas() > 0);
    }

    // ---- generateSimonFile ----

    @Test
    void generateSimonFile_createsWorkbook() throws Exception {
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of(approvedRelation));
        when(simonFileGenerationRepository.save(any(SimonFileGeneration.class)))
                .thenReturn(new SimonFileGeneration());

        Workbook wb = simonFileService.generateSimonFile("admin", "2025-1");

        assertNotNull(wb);
        assertEquals(1, wb.getNumberOfSheets());
        assertEquals(2, wb.getSheetAt(0).getPhysicalNumberOfRows()); // header + data
        assertEquals("Carlos", wb.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
        wb.close();
    }

    @Test
    void generateSimonFile_noApproved_throwsException() {
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> simonFileService.generateSimonFile("admin", "2025-1"));
    }

    // ---- getGenerationHistory and getGenerationHistoryBySemester (legacy) ----

    @Test
    void getGenerationHistory_legacy_returnsAll() {
        when(simonFileGenerationRepository.findAllByOrderByGeneratedAtDesc())
                .thenReturn(List.of(new SimonFileGeneration()));

        List<SimonFileGeneration> result = simonFileService.getGenerationHistory();

        assertEquals(1, result.size());
    }

    @Test
    void getGenerationHistoryBySemester_returnsFiltered() {
        when(simonFileGenerationRepository.findBySemesterOrderByGeneratedAtDesc("2025-1"))
                .thenReturn(List.of(new SimonFileGeneration()));

        List<SimonFileGeneration> result = simonFileService.getGenerationHistoryBySemester("2025-1");

        assertEquals(1, result.size());
    }

    @Test
    void getGenerationHistoryBySemester_noResults_returnsEmpty() {
        when(simonFileGenerationRepository.findBySemesterOrderByGeneratedAtDesc("2024-2"))
                .thenReturn(List.of());

        List<SimonFileGeneration> result = simonFileService.getGenerationHistoryBySemester("2024-2");

        assertTrue(result.isEmpty());
    }
}
