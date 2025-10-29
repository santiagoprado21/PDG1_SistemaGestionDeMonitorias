package com.pdg.sigma.service;

import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.MonitoringMonitor;
import com.pdg.sigma.domain.SimonFileGeneration;
import com.pdg.sigma.dto.SimonMonitoringDTO;
import com.pdg.sigma.dto.SimonMonitoringRowDTO;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.SimonFileGenerationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SimonFileServiceImpl implements SimonFileService {

    private final MonitoringMonitorRepository monitoringMonitorRepository;
    private final SimonFileGenerationRepository simonFileGenerationRepository;

    public SimonFileServiceImpl(MonitoringMonitorRepository monitoringMonitorRepository,
                                SimonFileGenerationRepository simonFileGenerationRepository) {
        this.monitoringMonitorRepository = monitoringMonitorRepository;
        this.simonFileGenerationRepository = simonFileGenerationRepository;
    }

    @Override
    public List<SimonMonitoringRowDTO> getApprovedMonitoringsForSimon(Optional<String> semester) {
        List<MonitoringMonitor> approved = monitoringMonitorRepository.findByEstadoSeleccion("aprobado");
        List<SimonMonitoringRowDTO> rows = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");

        for (MonitoringMonitor mm : approved) {
            Monitoring m = mm.getMonitoring();
            Monitor monitor = mm.getMonitor();
            if (m == null || monitor == null) continue;

            if (semester.isPresent() && (m.getSemester() == null || !m.getSemester().equalsIgnoreCase(semester.get()))) {
                continue;
            }

            String start = m.getStart() != null ? fmt.format(m.getStart()) : "";
            String end = m.getFinish() != null ? fmt.format(m.getFinish()) : "";
            Integer horas = m.getEstimatedHours() != null ? m.getEstimatedHours() : 0;

            SimonMonitoringRowDTO dto = new SimonMonitoringRowDTO();
            dto.setNombre(monitor.getName());
            dto.setApellido(monitor.getLastName());
            dto.setCodigoEstudiante(monitor.getCode());
            dto.setEmail(monitor.getEmail());
            dto.setNombreCurso(m.getCourse() != null ? m.getCourse().getName() : "");
            dto.setProfesorSolicita(m.getProfessor() != null ? m.getProfessor().getName() : "");
            dto.setFechaInicio(start);
            dto.setFechaFin(end);
            dto.setTotalHoras(horas);
            dto.setSemestre(m.getSemester());
            dto.setFacultad(m.getSchool() != null ? m.getSchool().getName() : "");
            dto.setPrograma(m.getProgram() != null ? m.getProgram().getName() : "");
            dto.setCodigoCurso(m.getCourse() != null ? String.valueOf(m.getCourse().getId()) : "");
            dto.setIdProfesor(m.getProfessor() != null ? m.getProfessor().getId() : "");
            dto.setCodigoMonitor(monitor.getIdMonitor());
            dto.setSemanas("");
            dto.setObservacion("");

            rows.add(dto);
        }
        return rows;
    }

    @Override
    public ByteArrayOutputStream buildSimonWorkbook(List<SimonMonitoringRowDTO> rows) throws Exception {
        String[] HEADERS = new String[]{
                "NOMBRE", "APELLIDO", "CODIGO", "EMAIL", "CURSO", "PROFESOR",
                "FECHA_INICIO", "FECHA_FIN", "TOTAL_HORAS", "SEMESTRE",
                "FACULTAD", "PROGRAMA", "COD_CURSO", "ID_PROFESOR", "COD_MONITOR",
                "SEMANAS", "OBSERVACION"
        };

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("SIMON");
            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            // Header row
            Row h = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            // Data
            int r = 1;
            for (SimonMonitoringRowDTO dto : rows) {
                Row row = sheet.createRow(r++);
                int col = 0;
                row.createCell(col++).setCellValue(nvl(dto.getNombre()));
                row.createCell(col++).setCellValue(nvl(dto.getApellido()));
                row.createCell(col++).setCellValue(nvl(dto.getCodigoEstudiante()));
                row.createCell(col++).setCellValue(nvl(dto.getEmail()));
                row.createCell(col++).setCellValue(nvl(dto.getNombreCurso()));
                row.createCell(col++).setCellValue(nvl(dto.getProfesorSolicita()));
                row.createCell(col++).setCellValue(nvl(dto.getFechaInicio()));
                row.createCell(col++).setCellValue(nvl(dto.getFechaFin()));
                row.createCell(col++).setCellValue(dto.getTotalHoras() == null ? 0 : dto.getTotalHoras());
                row.createCell(col++).setCellValue(nvl(dto.getSemestre()));
                row.createCell(col++).setCellValue(nvl(dto.getFacultad()));
                row.createCell(col++).setCellValue(nvl(dto.getPrograma()));
                row.createCell(col++).setCellValue(nvl(dto.getCodigoCurso()));
                row.createCell(col++).setCellValue(nvl(dto.getIdProfesor()));
                row.createCell(col++).setCellValue(nvl(dto.getCodigoMonitor()));
                row.createCell(col++).setCellValue(nvl(dto.getSemanas()));
                row.createCell(col++).setCellValue(nvl(dto.getObservacion()));
            }

            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out;
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }

    @Override
    public List<SimonFileGeneration> getGenerationHistory(Optional<String> semester) {
        if (semester.isPresent()) {
            return simonFileGenerationRepository.findBySemesterOrderByGeneratedAtDesc(semester.get());
        }
        return simonFileGenerationRepository.findAllByOrderByGeneratedAtDesc();
    }

    @Override
    public SimonFileGeneration auditGeneration(String generatedBy, String semester, int total, String fileName) {
        SimonFileGeneration record = new SimonFileGeneration(
                java.time.LocalDateTime.now(),
                generatedBy,
                total,
                fileName,
                semester
        );
        return simonFileGenerationRepository.save(record);
    }

    // ===================== Legacy/tested API implementations =====================

    @Override
    public org.apache.poi.ss.usermodel.Workbook generateSimonFile(String generatedBy, String semester) throws IOException {
        List<SimonMonitoringDTO> monitorings = getApprovedMonitoringsForSimon();
        if (monitorings.isEmpty()) {
            throw new IllegalStateException("No hay monitorías aprobadas para generar el archivo");
        }

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Monitorías SIMON");

        // Headers exactos requeridos por HU4/tests
        String[] HEADERS = new String[]{
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

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i, CellType.STRING).setCellValue(HEADERS[i]);
        }

        int r = 1;
        for (SimonMonitoringDTO dto : monitorings) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(nvl(dto.getEstudianteTipo()));
            row.createCell(c++).setCellValue(nvl(dto.getCenco()));
            row.createCell(c++).setCellValue(nvl(dto.getNumeroMonitoria()));
            row.createCell(c++).setCellValue(nvl(dto.getNombre()));
            row.createCell(c++).setCellValue(nvl(dto.getApellido()));
            row.createCell(c++).setCellValue(nvl(dto.getCedula()));
            row.createCell(c++).setCellValue(nvl(dto.getCodigoEstudiante()));
            row.createCell(c++).setCellValue(nvl(dto.getEmail()));
            row.createCell(c++).setCellValue(nvl(dto.getCelular()));
            row.createCell(c++).setCellValue(nvl(dto.getCodigoCurso()));
            row.createCell(c++).setCellValue(nvl(dto.getNombreCurso()));
            row.createCell(c++).setCellValue(nvl(dto.getDescripcionMonitoria()));
            row.createCell(c++).setCellValue(nvl(dto.getFechaInicio()));
            row.createCell(c++).setCellValue(nvl(dto.getFechaFin()));
            row.createCell(c++).setCellValue(dto.getTotalHoras() == null ? 0 : dto.getTotalHoras());
            row.createCell(c++).setCellValue(dto.getTotalSemanas() == null ? 0 : dto.getTotalSemanas());
            row.createCell(c++).setCellValue(nvl(dto.getProfesorSolicita()));
        }

        for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);

        // Registrar generación en BD
        String fileName = "SIMON_" + semester + "_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        simonFileGenerationRepository.save(new SimonFileGeneration(
                java.time.LocalDateTime.now(),
                generatedBy,
                monitorings.size(),
                fileName,
                semester
        ));

        return wb;
    }

    @Override
    public List<SimonMonitoringDTO> getApprovedMonitoringsForSimon() {
        List<MonitoringMonitor> approved = monitoringMonitorRepository.findByEstadoSeleccion("aprobado");
        List<SimonMonitoringDTO> dtos = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (MonitoringMonitor mm : approved) {
            Monitoring m = mm.getMonitoring();
            Monitor mon = mm.getMonitor();
            if (m == null || mon == null) continue;

            SimonMonitoringDTO dto = new SimonMonitoringDTO();
            dto.setEstudianteTipo("PRE");
            dto.setCenco("CA021202");
            dto.setNumeroMonitoria("");
            dto.setNombre(nvl(mon.getName()));
            dto.setApellido(nvl(mon.getLastName()));
            dto.setCedula(nvl(mon.getIdMonitor()));
            dto.setCodigoEstudiante(nvl(mon.getCode()));
            dto.setEmail(nvl(mon.getEmail()));
            dto.setCelular("");

            String codigoCurso = m.getCourse() != null && m.getCourse().getId() != null
                    ? "TIC - " + m.getCourse().getId()
                    : "";
            dto.setCodigoCurso(codigoCurso);
            dto.setNombreCurso(m.getCourse() != null ? nvl(m.getCourse().getName()) : "");
            dto.setDescripcionMonitoria("NRC-1");

            dto.setFechaInicio(m.getStart() != null ? sdf.format(m.getStart()) : "");
            dto.setFechaFin(m.getFinish() != null ? sdf.format(m.getFinish()) : "");

            Integer horas = m.getEstimatedHours() != null ? m.getEstimatedHours() : 30;
            dto.setTotalHoras(horas);

            // Calcular semanas entre fechas (entero)
            Integer semanas = 0;
            if (m.getStart() != null && m.getFinish() != null) {
                java.time.LocalDate s = m.getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                java.time.LocalDate e = m.getFinish().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                long w = ChronoUnit.WEEKS.between(s, e);
                semanas = (int) Math.max(0, w);
            }
            dto.setTotalSemanas(semanas);

            dto.setProfesorSolicita(m.getProfessor() != null ? nvl(m.getProfessor().getName()) : "");

            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public List<SimonFileGeneration> getGenerationHistory() {
        return simonFileGenerationRepository.findAllByOrderByGeneratedAtDesc();
    }

    @Override
    public List<SimonFileGeneration> getGenerationHistoryBySemester(String semester) {
        return simonFileGenerationRepository.findBySemesterOrderByGeneratedAtDesc(semester);
    }
}

