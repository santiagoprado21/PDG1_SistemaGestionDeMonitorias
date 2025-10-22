package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.SimonMonitoringDTO;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.SimonFileGenerationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class SimonFileServiceImpl implements SimonFileService {

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Autowired
    private SimonFileGenerationRepository simonFileGenerationRepository;

    private static final String[] HEADERS = {
        "Estudiante de PRE/POS",
        "CENCO",
        "No.Monitoria",
        "NOMBRE",
        "APELLIDO",
        "CÉDULA",
        "CÓDIGO DE ESTUDIANTE",
        "EMAIL",
        "NÚMERO DE CELULAR\nDAVIPLATA",
        "Código curso",
        "CURSO O PROYECTO",
        "DESCRIPCIÓN MONITORÍA",
        "FECHA\nINICIO",
        "FECHA\nFIN",
        "TOTAL\nHORAS",
        "Total Semanas",
        "Profesor que solicita la monitoria"
    };

    @Override
    public Workbook generateSimonFile(String generatedBy, String semester) throws IOException {
        // 1. Obtener monitorías aprobadas
        List<SimonMonitoringDTO> monitorings = getApprovedMonitoringsForSimon();

        if (monitorings.isEmpty()) {
            throw new IllegalStateException("No hay monitorías aprobadas para generar el archivo");
        }

        // 2. Crear el archivo Excel
        Workbook workbook = createExcelFile(monitorings);

        // 3. Registrar la generación
        String fileName = "SIMON_" + semester + "_" + 
                         LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        
        SimonFileGeneration generation = new SimonFileGeneration(
            LocalDateTime.now(),
            generatedBy,
            monitorings.size(),
            fileName,
            semester
        );
        
        simonFileGenerationRepository.save(generation);

        return workbook;
    }

    @Override
    public List<SimonMonitoringDTO> getApprovedMonitoringsForSimon() {
        // Obtener todas las postulaciones aprobadas por el jefe
        List<MonitoringMonitor> approvedMonitorings = 
            monitoringMonitorRepository.findByEstadoSeleccion("aprobado");

        List<SimonMonitoringDTO> simonDTOs = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        for (MonitoringMonitor mm : approvedMonitorings) {
            Monitor monitor = mm.getMonitor();
            Monitoring monitoring = mm.getMonitoring();
            Course course = monitoring.getCourse();
            Professor professor = monitoring.getProfessor();

            SimonMonitoringDTO dto = new SimonMonitoringDTO();

            // 1. Estudiante de PRE/POS - Por defecto PRE (pregrado)
            dto.setEstudianteTipo("PRE");

            // 2. CENCO - Centro de costo (se puede configurar por programa o dejar vacío)
            dto.setCenco("CA021202"); // TODO: Configurar según programa/facultad

            // 3. No.Monitoria - Puede ser vacío o ID
            dto.setNumeroMonitoria("");

            // 4. NOMBRE
            dto.setNombre(monitor.getName());

            // 5. APELLIDO
            dto.setApellido(monitor.getLastName());

            // 6. CÉDULA - Usar el ID del monitor
            dto.setCedula(monitor.getIdMonitor());

            // 7. CÓDIGO DE ESTUDIANTE
            dto.setCodigoEstudiante(monitor.getCode());

            // 8. EMAIL
            dto.setEmail(monitor.getEmail());

            // 9. NÚMERO DE CELULAR DAVIPLATA - Por ahora vacío
            dto.setCelular(""); // TODO: Agregar campo de celular al Monitor

            // 10. Código curso - Formato: "TIC - 09704"
            dto.setCodigoCurso("TIC - " + course.getId());

            // 11. CURSO O PROYECTO
            dto.setNombreCurso(course.getName());

            // 12. DESCRIPCIÓN MONITORÍA - Usar semestre y NRC
            dto.setDescripcionMonitoria("NRC-" + monitoring.getId());

            // 13. FECHA INICIO
            dto.setFechaInicio(dateFormat.format(monitoring.getStart()));

            // 14. FECHA FIN
            dto.setFechaFin(dateFormat.format(monitoring.getFinish()));

            // 15. TOTAL HORAS - Calcular o usar valor fijo
            dto.setTotalHoras(30); // TODO: Calcular según las horas configuradas

            // 16. Total Semanas - Calcular entre fechas
            long weeks = calculateWeeks(monitoring.getStart(), monitoring.getFinish());
            dto.setTotalSemanas((int) weeks);

            // 17. Profesor que solicita la monitoria
            dto.setProfesorSolicita(professor.getName());

            simonDTOs.add(dto);
        }

        return simonDTOs;
    }

    @Override
    public List<SimonFileGeneration> getGenerationHistory() {
        return simonFileGenerationRepository.findAllByOrderByGeneratedAtDesc();
    }

    @Override
    public List<SimonFileGeneration> getGenerationHistoryBySemester(String semester) {
        return simonFileGenerationRepository.findBySemesterOrderByGeneratedAtDesc(semester);
    }

    /**
     * Crea el archivo Excel con los datos de las monitorías
     */
    private Workbook createExcelFile(List<SimonMonitoringDTO> monitorings) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Monitorías SIMON");

        // Crear estilos
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Crear fila de encabezados
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
            
            // Ajustar ancho de columnas
            sheet.setColumnWidth(i, 4000);
        }

        // Llenar datos
        int rowNum = 1;
        for (SimonMonitoringDTO monitoring : monitorings) {
            Row row = sheet.createRow(rowNum++);

            setCellValue(row, 0, monitoring.getEstudianteTipo(), dataStyle);
            setCellValue(row, 1, monitoring.getCenco(), dataStyle);
            setCellValue(row, 2, monitoring.getNumeroMonitoria(), dataStyle);
            setCellValue(row, 3, monitoring.getNombre(), dataStyle);
            setCellValue(row, 4, monitoring.getApellido(), dataStyle);
            setCellValue(row, 5, monitoring.getCedula(), dataStyle);
            setCellValue(row, 6, monitoring.getCodigoEstudiante(), dataStyle);
            setCellValue(row, 7, monitoring.getEmail(), dataStyle);
            setCellValue(row, 8, monitoring.getCelular(), dataStyle);
            setCellValue(row, 9, monitoring.getCodigoCurso(), dataStyle);
            setCellValue(row, 10, monitoring.getNombreCurso(), dataStyle);
            setCellValue(row, 11, monitoring.getDescripcionMonitoria(), dataStyle);
            setCellValue(row, 12, monitoring.getFechaInicio(), dataStyle);
            setCellValue(row, 13, monitoring.getFechaFin(), dataStyle);
            setCellValue(row, 14, String.valueOf(monitoring.getTotalHoras()), dataStyle);
            setCellValue(row, 15, String.valueOf(monitoring.getTotalSemanas()), dataStyle);
            setCellValue(row, 16, monitoring.getProfesorSolicita(), dataStyle);
        }

        return workbook;
    }

    /**
     * Crea el estilo para los encabezados
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    /**
     * Crea el estilo para las celdas de datos
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Establece el valor de una celda con estilo
     */
    private void setCellValue(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * Calcula las semanas entre dos fechas
     */
    private long calculateWeeks(Date startDate, Date endDate) {
        LocalDateTime start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return ChronoUnit.WEEKS.between(start, end);
    }
}

