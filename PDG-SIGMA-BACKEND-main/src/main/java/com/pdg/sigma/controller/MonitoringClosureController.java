package com.pdg.sigma.controller;

import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.dto.MonitoringClosureReport;
import com.pdg.sigma.dto.MonitoringClosureRequest;
import com.pdg.sigma.service.MonitoringClosureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HU-007: Controlador REST para cierre de monitorías al final del semestre
 */
@RestController
@RequestMapping("/monitoring-closure")
public class MonitoringClosureController {

    @Autowired
    private MonitoringClosureService monitoringClosureService;

    /**
     * Obtiene monitorías listas para cerrar (estado APROBADA)
     * GET /monitoring-closure/ready-for-closure?semester=2026-1&programId=1
     */
    @GetMapping("/ready-for-closure")
    public ResponseEntity<?> getMonitoringsReadyForClosure(
            @RequestParam String semester,
            @RequestParam(required = false) Integer programId) {
        try {
            List<Monitoring> monitorings = monitoringClosureService.getMonitoringsReadyForClosure(semester, programId);
            return ResponseEntity.ok(monitorings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cierra una monitoría individual
     * POST /monitoring-closure/{id}/close
     * Body: { "comment": "...", "autoCalculate": true }
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeMonitoring(
            @PathVariable Long id,
            @RequestBody MonitoringClosureRequest request,
            @RequestParam String directorId) {
        try {
            MonitoringClosureReport report = monitoringClosureService.closeMonitoring(id, request, directorId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cierra múltiples monitorías en lote
     * POST /monitoring-closure/close-batch
     * Body: { "monitoringIds": [1, 2, 3], "directorId": "...", "closureData": { "comment": "...", "autoCalculate": true } }
     */
    @PostMapping("/close-batch")
    public ResponseEntity<?> closeMonitoringsBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> monitoringIds = (List<Long>) request.get("monitoringIds");
            String directorId = (String) request.get("directorId");
            @SuppressWarnings("unchecked")
            Map<String, Object> closureDataMap = (Map<String, Object>) request.get("closureData");

            if (monitoringIds == null || monitoringIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Se requiere al menos una monitoría para cerrar"));
            }

            if (directorId == null || directorId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Se requiere el ID del director"));
            }

            // Crear MonitoringClosureRequest desde el mapa
            MonitoringClosureRequest closureRequest = new MonitoringClosureRequest();
            if (closureDataMap != null) {
                closureRequest.setComment((String) closureDataMap.get("comment"));
                closureRequest.setAutoCalculate((Boolean) closureDataMap.getOrDefault("autoCalculate", true));
            } else {
                closureRequest.setComment("Cierre en lote del semestre");
                closureRequest.setAutoCalculate(true);
            }

            List<MonitoringClosureReport> reports = monitoringClosureService.closeMonitoringsBatch(monitoringIds, directorId, closureRequest);
            return ResponseEntity.ok(Map.of(
                    "message", "Monitorías cerradas exitosamente",
                    "closed", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Genera reporte de cumplimiento para una monitoría
     * GET /monitoring-closure/{id}/report
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<?> getComplianceReport(@PathVariable Long id) {
        try {
            MonitoringClosureReport report = monitoringClosureService.generateComplianceReport(id);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtiene monitorías cerradas de un semestre
     * GET /monitoring-closure/closed?semester=2026-1&programId=1
     */
    @GetMapping("/closed")
    public ResponseEntity<?> getClosedMonitorings(
            @RequestParam String semester,
            @RequestParam(required = false) Integer programId) {
        try {
            List<Monitoring> monitorings = monitoringClosureService.getClosedMonitorings(semester, programId);
            return ResponseEntity.ok(monitorings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
