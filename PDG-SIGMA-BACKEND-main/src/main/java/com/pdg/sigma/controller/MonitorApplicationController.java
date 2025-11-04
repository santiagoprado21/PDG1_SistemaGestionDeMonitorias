package com.pdg.sigma.controller;

import com.pdg.sigma.domain.MonitorApplication;
import com.pdg.sigma.domain.MonitoringRequest;
import com.pdg.sigma.dto.MonitorApplicationDTO;
import com.pdg.sigma.dto.SelectMonitorRequest;
import com.pdg.sigma.service.MonitorApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para gestionar MonitorApplication (Postulaciones de Estudiantes)
 * Parte del flujo de HU-010
 */
@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RequestMapping("/monitor-application")
@RestController
public class MonitorApplicationController {

    @Autowired
    private MonitorApplicationService monitorApplicationService;

    /**
     * POST /monitor-application/apply
     * Un estudiante se postula a una convocatoria
     * Body: MonitorApplicationDTO { monitoringRequestId, monitorId, motivationLetter? }
     */
    @PostMapping("/apply")
    public ResponseEntity<?> applyToConvocatoria(@RequestBody MonitorApplicationDTO dto) {
        try {
            MonitorApplication application = monitorApplicationService.applyToConvocatoria(dto);
            MonitorApplicationDTO response = new MonitorApplicationDTO(application);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.err.println("Error al postularse: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitor-application/request/{requestId}
     * Obtiene todos los postulantes de una convocatoria
     * (Para que el profesor vea quiénes se postularon)
     */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<?> getApplicationsByRequest(@PathVariable Long requestId) {
        try {
            List<MonitorApplication> applications = monitorApplicationService.getApplicationsByRequest(requestId);
            
            List<MonitorApplicationDTO> dtos = applications.stream()
                    .map(MonitorApplicationDTO::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitor-application/monitor/{monitorId}
     * Obtiene todas las postulaciones de un estudiante
     * (Para que el estudiante vea a qué convocatorias se ha postulado)
     */
    @GetMapping("/monitor/{monitorId}")
    public ResponseEntity<?> getApplicationsByMonitor(@PathVariable String monitorId) {
        try {
            List<MonitorApplication> applications = monitorApplicationService.getApplicationsByMonitor(monitorId);
            
            List<MonitorApplicationDTO> dtos = applications.stream()
                    .map(MonitorApplicationDTO::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /monitor-application/select
     * El profesor selecciona un monitor de los postulantes
     * Body: SelectMonitorRequest { monitoringRequestId, applicationId, professorId, notes? }
     * 
     * Este endpoint:
     * 1. Marca la postulación como SELECCIONADO
     * 2. Marca las demás como NO_SELECCIONADO
     * 3. Crea la Monitoring con monitor asignado
     * 4. Actualiza estados de MonitoringRequest
     */
    @PostMapping("/select")
    public ResponseEntity<?> selectMonitor(@RequestBody SelectMonitorRequest request) {
        try {
            monitorApplicationService.selectMonitor(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Monitor seleccionado exitosamente. La monitoría ha sido enviada para aprobación del jefe de departamento."
            ));
        } catch (Exception e) {
            System.err.println("Error al seleccionar monitor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /monitor-application/{applicationId}
     * Un estudiante cancela su postulación
     * Query param: monitorId
     */
    @DeleteMapping("/{applicationId}")
    public ResponseEntity<?> cancelApplication(
            @PathVariable Long applicationId,
            @RequestParam String monitorId) {
        try {
            monitorApplicationService.cancelApplication(applicationId, monitorId);
            return ResponseEntity.ok(Map.of("message", "Postulación cancelada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitor-application/check-applied/{requestId}/{monitorId}
     * Verifica si un estudiante ya se postuló a una convocatoria
     */
    @GetMapping("/check-applied/{requestId}/{monitorId}")
    public ResponseEntity<?> checkIfApplied(
            @PathVariable Long requestId,
            @PathVariable String monitorId) {
        try {
            boolean hasApplied = monitorApplicationService.hasApplied(requestId, monitorId);
            return ResponseEntity.ok(Map.of("hasApplied", hasApplied));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitor-application/available/{monitorId}/{programId}
     * Obtiene convocatorias disponibles para un estudiante
     * (Abiertas, de su programa, donde no se ha postulado)
     */
    @GetMapping("/available/{monitorId}/{programId}")
    public ResponseEntity<?> getAvailableConvocatorias(
            @PathVariable String monitorId,
            @PathVariable Integer programId) {
        try {
            List<MonitoringRequest> available = monitorApplicationService
                    .getAvailableConvocatoriasForMonitor(monitorId, programId);
            
            // Convertir a DTO simplificado
            List<Map<String, Object>> dtos = available.stream()
                    .map(request -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", request.getId());
                        map.put("courseName", request.getCourse() != null ? request.getCourse().getName() : "");
                        map.put("professorName", request.getProfessor() != null ? request.getProfessor().getName() : "");
                        map.put("requestedHours", request.getRequestedHours());
                        map.put("semester", request.getSemester());
                        map.put("requiredAverageGrade", request.getRequiredAverageGrade() != null ? request.getRequiredAverageGrade() : 0.0);
                        map.put("requiredCourseGrade", request.getRequiredCourseGrade() != null ? request.getRequiredCourseGrade() : 0.0);
                        return map;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitor-application/{id}
     * Obtiene una postulación específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getApplicationById(@PathVariable Long id) {
        try {
            MonitorApplication application = monitorApplicationService.findById(id)
                    .orElseThrow(() -> new Exception("Postulación no encontrada"));
            
            MonitorApplicationDTO dto = new MonitorApplicationDTO(application);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitor-application/all
     * Obtiene todas las postulaciones (admin)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllApplications() {
        try {
            List<MonitorApplication> applications = monitorApplicationService.findAll();
            List<MonitorApplicationDTO> dtos = applications.stream()
                    .map(MonitorApplicationDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

