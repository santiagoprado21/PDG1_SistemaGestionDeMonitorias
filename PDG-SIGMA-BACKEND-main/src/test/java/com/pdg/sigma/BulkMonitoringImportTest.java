package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.MonitoringServiceImpl;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BulkMonitoringImportTest {

    // Mock mail sender to satisfy EmailSenderService dependency in test context
    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired private SchoolRepository schoolRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private StudentCourseRepository studentCourseRepository;
    @Autowired private MonitoringRepository monitoringRepository;

    @Autowired private MonitoringServiceImpl monitoringService;

    private School school;
    private Program program;
    private Course courseLow;
    private Course courseHigh;
    private Professor professor;

    @BeforeEach
    void setup() {
        // Clean state
        monitoringRepository.deleteAll();
        studentCourseRepository.deleteAll();
        courseRepository.deleteAll();
        programRepository.deleteAll();
        schoolRepository.deleteAll();
        professorRepository.deleteAll();

        // Seed base academic structure
        school = new School();
        school.setName("Facultad de Ingeniería");
        school = schoolRepository.save(school);

        program = new Program();
        program.setName("Ingeniería de Sistemas");
        program.setSchool(school);
        program = programRepository.save(program);

        courseLow = new Course();
        courseLow.setName("Programación I");
        courseLow.setProgram(program);
        courseLow = courseRepository.save(courseLow);

        courseHigh = new Course();
        courseHigh.setName("Bases de Datos");
        courseHigh.setProgram(program);
        courseHigh = courseRepository.save(courseHigh);

        // Professor needed by service
        professor = new Professor();
        professor.setId("1001");
        professor.setName("Dr. Test");
        professor.setPassword("prof123");
        professorRepository.save(professor);

        // Enroll students: 14 for courseLow, 15 for courseHigh
        int lowId = Math.toIntExact(courseLow.getId());
        int highId = Math.toIntExact(courseHigh.getId());
        for (int i = 1; i <= 14; i++) {
            studentCourseRepository.save(new StudentCourse(null, lowId, "SLOW" + i));
        }
        for (int i = 1; i <= 15; i++) {
            studentCourseRepository.save(new StudentCourse(null, highId, "SHIGH" + i));
        }
    }

    @Test
    void acceptsCsv_creates_one_and_omits_rest_by_professor() throws Exception {
        String header = "FACULTAD,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA\n";
        String r1 = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-2,4.5,4.6,8,10000\n",
                school.getName(), program.getName(), courseLow.getName());
        String r2 = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-2,4.5,4.6,8,10000\n",
                school.getName(), program.getName(), courseHigh.getName());
        byte[] bytes = (header + r1 + r2).getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "monitorias.csv", "text/csv", bytes);

        String result = monitoringService.processListMonitor(file, professor.getId());

        // Sin regla 15+: se crea una y el resto omitido por unicidad por profesor
        assertTrue(result.contains("Creadas"), "Debe listar Creadas");
        assertTrue(result.contains("Omitidas"), "Debe listar Omitidas por unicidad del profesor");
        assertTrue(result.contains(courseHigh.getName()));
        assertTrue(result.contains(courseLow.getName()));
        assertEquals(1, monitoringRepository.count(), "Debe existir solo una monitoría creada");
    }

    @Test
    void rejectsCsv_with_invalid_header() {
        String badHeader = "FACULTADX,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA\n";
        String row = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-2,4.5,4.6,8,10000\n",
                school.getName(), program.getName(), courseLow.getName());
        byte[] bytes = (badHeader + row).getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "monitorias.csv", "text/csv", bytes);

        Exception ex = assertThrows(Exception.class, () ->
                monitoringService.processListMonitor(file, professor.getId()));
        assertTrue(ex.getMessage().toLowerCase().contains("incompatibilidad"));
    }

    @Test
    void acceptsExcel_creates_one_and_omits_rest_by_professor() throws Exception {
        // Prepare Excel workbook in-memory
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");
        Row h = sheet.createRow(0);
        String[] headers = new String[]{
                "FACULTAD","PROGRAMA","CURSO","FECHA INICIO","FECHA FINALIZACION","PERIODO",
                "PROMEDIO ACUMULADO","PROMEDIO MATERIA","HORAS ESTIMADAS","VALOR HORA"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = h.createCell(i); c.setCellValue(headers[i]);
        }
        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue(school.getName());
        r1.createCell(1).setCellValue(program.getName());
        r1.createCell(2).setCellValue(courseLow.getName());
        r1.createCell(3).setCellValue("28-10-2025");
        r1.createCell(4).setCellValue("30-11-2025");
        r1.createCell(5).setCellValue("2025-2");
        r1.createCell(6).setCellValue("4.5");
        r1.createCell(7).setCellValue("4.6");
        r1.createCell(8).setCellValue("8");
        r1.createCell(9).setCellValue("10000");

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue(school.getName());
        r2.createCell(1).setCellValue(program.getName());
        r2.createCell(2).setCellValue(courseHigh.getName());
        r2.createCell(3).setCellValue("28-10-2025");
        r2.createCell(4).setCellValue("30-11-2025");
        r2.createCell(5).setCellValue("2025-2");
        r2.createCell(6).setCellValue("4.5");
        r2.createCell(7).setCellValue("4.6");
        r2.createCell(8).setCellValue("8");
        r2.createCell(9).setCellValue("10000");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();
        byte[] xlsx = bos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file", "monitorias.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsx);

        String result = monitoringService.processListMonitor(file, professor.getId());
        assertTrue(result.contains("Creadas"));
        assertTrue(result.contains("Omitidas"));
        assertTrue(result.contains(courseHigh.getName()) && result.contains(courseLow.getName()));
    }

    @Test
    void acceptsCsv_with_msexcel_contentType_creates_one_and_omits_rest() throws Exception {
    String header = "FACULTAD,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA\n";
    String r1 = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-2,4.5,4.6,8,10000\n",
        school.getName(), program.getName(), courseLow.getName());
    String r2 = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-2,4.5,4.6,8,10000\n",
        school.getName(), program.getName(), courseHigh.getName());
    byte[] bytes = (header + r1 + r2).getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile(
        "file", "monitorias.csv", "application/vnd.ms-excel", bytes);

    String result = monitoringService.processListMonitor(file, professor.getId());
    assertTrue(result.contains("Creadas"));
    assertTrue(result.contains("Omitidas"));
    assertTrue(result.contains(courseHigh.getName()) && result.contains(courseLow.getName()));
    }

    @Test
    void acceptsCsv_with_header_trailing_spaces() throws Exception {
    String header = "FACULTAD,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO   ,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA\n";
    String r = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-2,4.5,4.6,8,10000\n",
        school.getName(), program.getName(), courseHigh.getName());
    byte[] bytes = (header + r).getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile(
        "file", "monitorias.csv", "text/csv", bytes);

    String result = monitoringService.processListMonitor(file, professor.getId());
    assertTrue(result.contains("Creadas"));
    assertTrue(result.contains(courseHigh.getName()));
    }

    @Test
    void rejectsCsv_with_wrong_year_in_semester() {
    String header = "FACULTAD,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA\n";
    String row = String.format("%s,%s,%s,28-10-2025,30-11-2025,2024-2,4.5,4.6,8,10000\n",
        school.getName(), program.getName(), courseHigh.getName());
    byte[] bytes = (header + row).getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile(
        "file", "monitorias.csv", "text/csv", bytes);

    Exception ex = assertThrows(Exception.class, () ->
        monitoringService.processListMonitor(file, professor.getId()));
    assertTrue(ex.getMessage().toLowerCase().contains("año actual"));
    }

    @Test
    void rejectsCsv_with_invalid_semester_for_month() {
    String header = "FACULTAD,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA\n";
    String row = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-1,4.5,4.6,8,10000\n",
        school.getName(), program.getName(), courseHigh.getName());
    byte[] bytes = (header + row).getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile(
        "file", "monitorias.csv", "text/csv", bytes);

    Exception ex = assertThrows(Exception.class, () ->
        monitoringService.processListMonitor(file, professor.getId()));
    assertTrue(ex.getMessage().toLowerCase().contains("semestre actual"));
    }

    @Test
    void rejectsCsv_with_blank_field() {
    String header = "FACULTAD,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA\n";
    String row = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-2,4.5,4.6,8,\n",
        school.getName(), program.getName(), courseHigh.getName());
    byte[] bytes = (header + row).getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile(
        "file", "monitorias.csv", "text/csv", bytes);

    Exception ex = assertThrows(Exception.class, () ->
        monitoringService.processListMonitor(file, professor.getId()));
    assertTrue(ex.getMessage().toLowerCase().contains("incompatibilidad"));
    }

    @Test
    void csv_duplicate_course_is_omitted() throws Exception {
    String header = "FACULTAD,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA\n";
    String row = String.format("%s,%s,%s,28-10-2025,30-11-2025,2025-2,4.5,4.6,8,10000\n",
        school.getName(), program.getName(), courseHigh.getName());
    byte[] bytes = (header + row).getBytes(StandardCharsets.UTF_8);

    MockMultipartFile f1 = new MockMultipartFile("file", "monitorias.csv", "text/csv", bytes);
    String first = monitoringService.processListMonitor(f1, professor.getId());
    assertTrue(first.contains("Creadas"));

    MockMultipartFile f2 = new MockMultipartFile("file", "monitorias.csv", "text/csv", bytes);
    String second = monitoringService.processListMonitor(f2, professor.getId());
    assertTrue(second.contains("Omitidas"));
    assertTrue(second.toLowerCase().contains("existía"));
    }

    @Test
    void rejectsExcel_with_invalid_header() throws Exception {
    XSSFWorkbook wb = new XSSFWorkbook();
    Sheet sheet = wb.createSheet("Sheet1");
    Row h = sheet.createRow(0);
    String[] headers = new String[]{
        "FACULTAD","PROGRAMA","CURSO","FECHA INICIO","FECHA FINALIZACION","PERIODOX",
        "PROMEDIO ACUMULADO","PROMEDIO MATERIA","HORAS ESTIMADAS","VALOR HORA"
    };
    for (int i = 0; i < headers.length; i++) {
        Cell c = h.createCell(i); c.setCellValue(headers[i]);
    }
    Row r = sheet.createRow(1);
    r.createCell(0).setCellValue(school.getName());
    r.createCell(1).setCellValue(program.getName());
    r.createCell(2).setCellValue(courseHigh.getName());
    r.createCell(3).setCellValue("28-10-2025");
    r.createCell(4).setCellValue("30-11-2025");
    r.createCell(5).setCellValue("2025-2");
    r.createCell(6).setCellValue("4.5");
    r.createCell(7).setCellValue("4.6");
    r.createCell(8).setCellValue("8");
    r.createCell(9).setCellValue("10000");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    wb.write(bos);
    wb.close();
    byte[] xlsx = bos.toByteArray();

    MockMultipartFile file = new MockMultipartFile(
        "file", "monitorias.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        xlsx);

    Exception ex = assertThrows(Exception.class, () ->
        monitoringService.processListMonitor(file, professor.getId()));
    assertTrue(ex.getMessage().toLowerCase().contains("incompatibilidad"));
    }
}
