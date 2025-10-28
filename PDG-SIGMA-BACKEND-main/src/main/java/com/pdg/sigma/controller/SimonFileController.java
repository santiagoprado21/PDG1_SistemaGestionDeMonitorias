package com.pdg.sigma.controller;

import com.pdg.sigma.domain.SimonFileGeneration;
import com.pdg.sigma.dto.SimonMonitoringDTO;
import com.pdg.sigma.service.SimonFileService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simon")
public class SimonFileController {

    @Autowired
    private SimonFileService simonFileService;

    /**
     * Genera y descarga el archivo Excel para SIMON
     * @param generatedBy Usuario que genera el archivo
     * @param semester Semestre académico
     * @return Archivo Excel descargable
     */
    @GetMapping("/generate")
    public ResponseEntity<Resource> generateSimonFile(
            @RequestParam(required = false, defaultValue = "coordinador") String generatedBy,
            @RequestParam(required = false, defaultValue = "2024-2") String semester) {
        
        try {
            // Generar el archivo
            Workbook workbook = simonFileService.generateSimonFile(generatedBy, semester);

            // Convertir a ByteArray
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            // Nombre del archivo con fecha y hora
            String fileName = "SIMON_" + semester + "_" + 
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + 
                            ".xlsx";

            // Configurar headers para descarga
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);

        } catch (IllegalStateException e) {
            // No hay monitorías aprobadas
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene la vista previa de las monitorías que se incluirán en el archivo
     * @return Lista de monitorías aprobadas en formato SIMON
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewSimonData() {
        try {
            List<SimonMonitoringDTO> monitorings = simonFileService.getApprovedMonitoringsForSimon();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalMonitorings", monitorings.size());
            response.put("monitorings", monitorings);
            response.put("canGenerate", !monitorings.isEmpty());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("canGenerate", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtiene el historial de archivos generados
     * @return Lista de registros de generación
     */
    @GetMapping("/history")
    public ResponseEntity<List<SimonFileGeneration>> getGenerationHistory() {
        try {
            List<SimonFileGeneration> history = simonFileService.getGenerationHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene el historial por semestre
     * @param semester Semestre académico
     * @return Lista de registros de generación del semestre
     */
    @GetMapping("/history/{semester}")
    public ResponseEntity<List<SimonFileGeneration>> getGenerationHistoryBySemester(@PathVariable String semester) {
        try {
            List<SimonFileGeneration> history = simonFileService.getGenerationHistoryBySemester(semester);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

