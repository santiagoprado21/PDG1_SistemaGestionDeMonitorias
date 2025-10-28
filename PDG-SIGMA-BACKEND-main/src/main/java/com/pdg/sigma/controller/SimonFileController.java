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

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RestController
@RequestMapping("/simon")
public class SimonFileController {

    @Autowired
    private SimonFileService simonFileService;

    @GetMapping("/generate")
    public ResponseEntity<Resource> generateSimonFile(
            @RequestParam(required = false, defaultValue = "coordinador") String generatedBy,
            @RequestParam(required = false, defaultValue = "2024-2") String semester) {

        try {
            Workbook workbook = simonFileService.generateSimonFile(generatedBy, semester);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            String fileName = "SIMON_" + semester + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                    ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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

    @GetMapping("/history")
    public ResponseEntity<List<SimonFileGeneration>> getGenerationHistory() {
        try {
            List<SimonFileGeneration> history = simonFileService.getGenerationHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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


