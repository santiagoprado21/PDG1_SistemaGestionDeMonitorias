package com.pdg.sigma.controller;

import com.pdg.sigma.dto.SupervisorEvaluationRequest;
import com.pdg.sigma.dto.SupervisorEvaluationResponse;
import com.pdg.sigma.dto.SupervisorEvaluationStatusDTO;
import com.pdg.sigma.service.SupervisorEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RestController
@RequestMapping("/supervisor-evaluations")
public class SupervisorEvaluationController {

    @Autowired
    private SupervisorEvaluationService supervisorEvaluationService;

    @PostMapping
    public ResponseEntity<?> createEvaluation(@RequestBody SupervisorEvaluationRequest request) {
        try {
            SupervisorEvaluationResponse response = supervisorEvaluationService.createEvaluation(request.getMonitorIdentifier(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/monitor/{monitorIdentifier}/assignments")
    public ResponseEntity<?> getAssignmentsForMonitor(@PathVariable String monitorIdentifier) {
        try {
            List<SupervisorEvaluationStatusDTO> assignments = supervisorEvaluationService.getAssignmentsForMonitor(monitorIdentifier);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("no encontrado")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/coordinator")
    public ResponseEntity<?> getAllEvaluations(@RequestAttribute("role") String role) {
        if (!isCoordinator(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No está autorizado"));
        }
        try {
            List<SupervisorEvaluationResponse> evaluations = supervisorEvaluationService.getEvaluationsForCoordinator();
            return ResponseEntity.ok(evaluations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/coordinator/professor/{professorId}")
    public ResponseEntity<?> getEvaluationsByProfessor(@PathVariable String professorId, @RequestAttribute("role") String role) {
        if (!isCoordinator(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No está autorizado"));
        }
        try {
            List<SupervisorEvaluationResponse> evaluations = supervisorEvaluationService.getEvaluationsByProfessor(professorId);
            return ResponseEntity.ok(evaluations);
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("no encontrado")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/coordinator/monitor/{monitorIdentifier}")
    public ResponseEntity<?> getEvaluationsByMonitor(@PathVariable String monitorIdentifier, @RequestAttribute("role") String role) {
        if (!isCoordinator(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No está autorizado"));
        }
        try {
            List<SupervisorEvaluationResponse> evaluations = supervisorEvaluationService.getEvaluationsByMonitor(monitorIdentifier);
            return ResponseEntity.ok(evaluations);
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("no encontrado")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{evaluationId}")
    public ResponseEntity<?> getEvaluation(@PathVariable Long evaluationId, @RequestAttribute("role") String role) {
        if (!isCoordinator(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No está autorizado"));
        }
        return supervisorEvaluationService.getEvaluation(evaluationId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Evaluación no encontrada")));
    }

    private boolean isCoordinator(String role) {
        return role != null && "jfedpto".equalsIgnoreCase(role.trim());
    }
}
