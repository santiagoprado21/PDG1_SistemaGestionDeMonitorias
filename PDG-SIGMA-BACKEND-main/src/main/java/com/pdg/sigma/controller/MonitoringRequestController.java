package com.pdg.sigma.controller;

import com.pdg.sigma.domain.MonitoringRequest;
import com.pdg.sigma.dto.MonitoringRequestDTO;
import com.pdg.sigma.service.MonitoringRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para gestionar MonitoringRequest (Convocatorias de Monitoría)
 * HU-010: Crear postulación de monitorias por parte de los profesores
 */
@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RequestMapping("/monitoring-request")
@RestController
public class MonitoringRequestController {

    @Autowired
    private MonitoringRequestService monitoringRequestService;

    /**
     * POST /monitoring-request/create
     * Crea una nueva convocatoria de monitoría
     * Body: MonitoringRequestDTO
     */
    @PostMapping("/create")
    public ResponseEntity<?> createConvocatoria(@RequestBody MonitoringRequestDTO dto) {
        try {
            MonitoringRequest created = monitoringRequestService.createConvocatoria(dto);
            MonitoringRequestDTO response = new MonitoringRequestDTO(created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.err.println("Error al crear convocatoria: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitoring-request/open
     * Obtiene todas las convocatorias abiertas
     */
    @GetMapping("/open")
    public ResponseEntity<?> getOpenConvocatorias() {
        try {
            List<MonitoringRequest> requests = monitoringRequestService.findOpenConvocatorias();
            List<MonitoringRequestDTO> dtos = requests.stream()
                    .map(MonitoringRequestDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitoring-request/open/program/{programId}
     * Obtiene convocatorias abiertas de un programa específico
     */
    @GetMapping("/open/program/{programId}")
    public ResponseEntity<?> getOpenConvocatoriasByProgram(@PathVariable Integer programId) {
        try {
            List<MonitoringRequest> requests = monitoringRequestService.findOpenConvocatoriasByProgram(programId);
            List<MonitoringRequestDTO> dtos = requests.stream()
                    .map(MonitoringRequestDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitoring-request/professor/{professorId}
     * Obtiene todas las convocatorias de un profesor
     */
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<?> getConvocatoriasByProfessor(@PathVariable String professorId) {
        try {
            List<MonitoringRequest> requests = monitoringRequestService.findByProfessor(professorId);
            List<MonitoringRequestDTO> dtos = requests.stream()
                    .map(request -> {
                        MonitoringRequestDTO dto = new MonitoringRequestDTO(request);
                        // Agregar el número de postulantes
                        dto.setApplicationCount(monitoringRequestService.getApplicationCount(request.getId()));
                        return dto;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitoring-request/pending-approval/department-head/{departmentHeadId}
     * Obtiene convocatorias pendientes de aprobación para el jefe de departamento
     */
    @GetMapping("/pending-approval/department-head/{departmentHeadId}")
    public ResponseEntity<?> getPendingApprovalForDepartmentHead(@PathVariable String departmentHeadId) {
        try {
            List<MonitoringRequest> requests = monitoringRequestService
                    .findPendingApprovalForDepartmentHead(departmentHeadId);
            
            List<MonitoringRequestDTO> dtos = requests.stream()
                    .map(request -> {
                        MonitoringRequestDTO dto = new MonitoringRequestDTO(request);
                        // Agregar información del monitor seleccionado si existe
                        if (request.getCreatedMonitoring() != null && 
                            request.getCreatedMonitoring().getAssignedMonitor() != null) {
                            dto.setSelectedMonitorName(
                                request.getCreatedMonitoring().getAssignedMonitor().getName() + " " +
                                request.getCreatedMonitoring().getAssignedMonitor().getLastName()
                            );
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitoring-request/{id}
     * Obtiene una convocatoria por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getConvocatoriaById(@PathVariable Long id) {
        try {
            System.out.println("=== GET CONVOCATORIA BY ID: " + id + " ===");
            MonitoringRequest request = monitoringRequestService.findById(id)
                    .orElseThrow(() -> new Exception("Convocatoria no encontrada"));
            
            System.out.println("Found request, creating DTO...");
            MonitoringRequestDTO dto = new MonitoringRequestDTO(request);
            System.out.println("DTO created, getting application count...");
            dto.setApplicationCount(monitoringRequestService.getApplicationCount(id));
            
            System.out.println("Returning DTO...");
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.err.println("ERROR in getConvocatoriaById: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /monitoring-request/{id}/cancel
     * Cancela una convocatoria
     * Body: { "professorId": "..." }
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelConvocatoria(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String professorId = body.get("professorId");
            if (professorId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "professorId es requerido"));
            }
            
            monitoringRequestService.cancelConvocatoria(id, professorId);
            return ResponseEntity.ok(Map.of("message", "Convocatoria cancelada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /monitoring-request/all
     * Obtiene todas las convocatorias (admin)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllConvocatorias() {
        try {
            List<MonitoringRequest> requests = monitoringRequestService.findAll();
            List<MonitoringRequestDTO> dtos = requests.stream()
                    .map(MonitoringRequestDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /monitoring-request/{id}
     * Elimina una convocatoria
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConvocatoria(@PathVariable Long id) {
        try {
            monitoringRequestService.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Convocatoria eliminada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== NUEVOS ENDPOINTS: APROBACIÓN DEL JEFE AL INICIO ====================
    
    /**
     * GET /monitoring-request/pending-head-approval/{departmentHeadId}
     * Obtiene convocatorias pendientes de aprobación inicial del jefe
     * (estado PENDIENTE_APROBACION_JEFE)
     */
    @GetMapping("/pending-head-approval/{departmentHeadId}")
    public ResponseEntity<?> getPendingHeadApproval(@PathVariable String departmentHeadId) {
        try {
            List<MonitoringRequest> requests = monitoringRequestService.findPendingHeadApproval(departmentHeadId);
            List<MonitoringRequestDTO> dtos = requests.stream()
                    .map(MonitoringRequestDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * POST /monitoring-request/{id}/approve-by-head
     * El jefe aprueba una convocatoria (cambia a CONVOCATORIA_ABIERTA)
     * Body: { "departmentHeadId": "...", "comment": "..." }
     */
    @PostMapping("/{id}/approve-by-head")
    public ResponseEntity<?> approveByHead(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String departmentHeadId = body.get("departmentHeadId");
            String comment = body.get("comment");
            
            if (departmentHeadId == null || departmentHeadId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "departmentHeadId es requerido"));
            }
            
            if (comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El comentario es obligatorio"));
            }
            
            monitoringRequestService.approveByHead(id, departmentHeadId, comment);
            return ResponseEntity.ok(Map.of(
                    "message", "Convocatoria aprobada exitosamente. Ahora está abierta para postulaciones.",
                    "newStatus", "CONVOCATORIA_ABIERTA"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * POST /monitoring-request/{id}/reject-by-head
     * El jefe rechaza una convocatoria
     * Body: { "departmentHeadId": "...", "comment": "..." }
     */
    @PostMapping("/{id}/reject-by-head")
    public ResponseEntity<?> rejectByHead(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String departmentHeadId = body.get("departmentHeadId");
            String comment = body.get("comment");
            
            if (departmentHeadId == null || departmentHeadId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "departmentHeadId es requerido"));
            }
            
            if (comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El comentario es obligatorio"));
            }
            
            monitoringRequestService.rejectByHead(id, departmentHeadId, comment);
            return ResponseEntity.ok(Map.of(
                    "message", "Convocatoria rechazada exitosamente",
                    "newStatus", "RECHAZADA"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * PUT /monitoring-request/{id}/modify-by-head
     * El jefe modifica y aprueba una convocatoria
     * Body: MonitoringRequestDTO con campos a modificar + departmentHeadId + comment
     */
    @PutMapping("/{id}/modify-by-head")
    public ResponseEntity<?> modifyAndApproveByHead(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            String departmentHeadId = (String) body.get("departmentHeadId");
            String comment = (String) body.get("comment");
            
            if (departmentHeadId == null || departmentHeadId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "departmentHeadId es requerido"));
            }
            
            if (comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El comentario es obligatorio"));
            }
            
            // Crear DTO con las modificaciones
            MonitoringRequestDTO modifications = new MonitoringRequestDTO();
            
            if (body.containsKey("requestedHours")) {
                modifications.setRequestedHours(((Number) body.get("requestedHours")).intValue());
            }
            if (body.containsKey("justification")) {
                modifications.setJustification((String) body.get("justification"));
            }
            if (body.containsKey("startDate")) {
                // Parsear fecha desde string si es necesario
                modifications.setStartDate(new java.text.SimpleDateFormat("yyyy-MM-dd")
                        .parse((String) body.get("startDate")));
            }
            if (body.containsKey("finishDate")) {
                modifications.setFinishDate(new java.text.SimpleDateFormat("yyyy-MM-dd")
                        .parse((String) body.get("finishDate")));
            }
            if (body.containsKey("requiredAverageGrade")) {
                modifications.setRequiredAverageGrade(((Number) body.get("requiredAverageGrade")).doubleValue());
            }
            if (body.containsKey("requiredCourseGrade")) {
                modifications.setRequiredCourseGrade(((Number) body.get("requiredCourseGrade")).doubleValue());
            }
            if (body.containsKey("hourlyRate")) {
                modifications.setHourlyRate(((Number) body.get("hourlyRate")).doubleValue());
            }
            
            MonitoringRequest updated = monitoringRequestService.modifyAndApproveByHead(
                    id, modifications, departmentHeadId, comment);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Convocatoria modificada y aprobada exitosamente",
                    "newStatus", "CONVOCATORIA_ABIERTA",
                    "data", new MonitoringRequestDTO(updated)
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

