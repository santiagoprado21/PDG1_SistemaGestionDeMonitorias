package com.pdg.sigma.controller;

import com.pdg.sigma.dto.MonitorEvaluationAssignmentDTO;
import com.pdg.sigma.dto.MonitorEvaluationRequest;
import com.pdg.sigma.dto.MonitorEvaluationResponse;
import com.pdg.sigma.service.MonitorEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RestController
@RequestMapping("/monitor-evaluations")
public class MonitorEvaluationController {

    @Autowired
    private MonitorEvaluationService monitorEvaluationService;

    @PostMapping
    public ResponseEntity<?> createEvaluation(@RequestBody MonitorEvaluationRequest request) {
        try {
            MonitorEvaluationResponse response = monitorEvaluationService.createEvaluation(request.getProfessorId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{evaluationId}")
    public ResponseEntity<?> updateEvaluation(@PathVariable Long evaluationId, @RequestBody MonitorEvaluationRequest request) {
        try {
            MonitorEvaluationResponse response = monitorEvaluationService.updateEvaluation(evaluationId, request.getProfessorId(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().toLowerCase().contains("no está autorizado")
                    ? HttpStatus.FORBIDDEN
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/professor/{professorId}")
    public ResponseEntity<?> getEvaluationsByProfessor(@PathVariable String professorId) {
        try {
            List<MonitorEvaluationResponse> evaluations = monitorEvaluationService.getEvaluationsByProfessor(professorId);
            return ResponseEntity.ok(evaluations);
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("no encontrado")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/professor/{professorId}/assignments")
    public ResponseEntity<?> getAssignments(@PathVariable String professorId, @RequestParam Optional<String> search) {
        try {
            List<MonitorEvaluationAssignmentDTO> assignments = monitorEvaluationService.getEvaluationAssignmentsForProfessor(professorId, search);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("no encontrado")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/monitor/{monitorIdentifier}")
    public ResponseEntity<?> getEvaluationsForMonitor(@PathVariable String monitorIdentifier) {
        try {
            List<MonitorEvaluationResponse> evaluations = monitorEvaluationService.getEvaluationsForMonitor(monitorIdentifier);
            return ResponseEntity.ok(evaluations);
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("no encontrado")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{evaluationId}/acknowledge")
    public ResponseEntity<?> acknowledgeEvaluation(@PathVariable Long evaluationId, @RequestBody Map<String, String> body) {
        try {
            String monitorIdentifier = body != null ? body.get("monitorIdentifier") : null;
            MonitorEvaluationResponse response = monitorEvaluationService.acknowledgeEvaluation(evaluationId, monitorIdentifier);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if (e.getMessage() != null && e.getMessage().contains("no encontrada")) {
                status = HttpStatus.NOT_FOUND;
            } else if (e.getMessage() != null && e.getMessage().contains("no pertenece")) {
                status = HttpStatus.FORBIDDEN;
            }
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{evaluationId}")
    public ResponseEntity<?> getEvaluation(@PathVariable Long evaluationId) {
        return monitorEvaluationService.getEvaluation(evaluationId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Evaluación no encontrada")));
    }
}
