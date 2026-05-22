package com.pdg.sigma.controller;

import com.pdg.sigma.dto.*;
import com.pdg.sigma.service.MonitorSurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RestController
@RequestMapping("/monitor-survey")
public class MonitorSurveyController {

    @Autowired
    private MonitorSurveyService monitorSurveyService;

    @GetMapping("/admin/questions")
    public ResponseEntity<?> getQuestionBank(@RequestAttribute("role") String role,
                                             @RequestParam(required = false) String semester) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            List<MonitorSurveyQuestionDTO> questions = monitorSurveyService.getQuestionBank(semester);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PostMapping("/admin/questions")
    public ResponseEntity<?> createQuestion(@RequestAttribute("role") String role,
                                            @RequestBody MonitorSurveyQuestionCreateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(monitorSurveyService.createQuestion(request));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PutMapping("/admin/questions/{questionId}")
    public ResponseEntity<?> updateQuestion(@RequestAttribute("role") String role,
                                            @PathVariable Long questionId,
                                            @RequestParam(required = false) String semester,
                                            @RequestBody MonitorSurveyQuestionUpdateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(monitorSurveyService.updateQuestion(questionId, request, semester));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PatchMapping("/admin/questions/{questionId}/status")
    public ResponseEntity<?> updateQuestionStatus(@RequestAttribute("role") String role,
                                                  @PathVariable Long questionId,
                                                  @RequestBody MonitorSurveyQuestionStatusRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            if (request.getBankActive() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Debe indicar el estado de la pregunta"));
            }
            return ResponseEntity.ok(monitorSurveyService.updateQuestionStatus(questionId, request.getBankActive()));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/admin/current-config")
    public ResponseEntity<?> getCurrentConfig(@RequestAttribute("role") String role,
                                              @RequestParam(required = false) String semester) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(monitorSurveyService.getCurrentConfig(semester));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/admin/validate-period")
    public ResponseEntity<?> validatePeriod(@RequestAttribute("role") String role,
                                            @RequestParam String semester) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(Map.of("valid", true, "semester", normalizeAllowedPeriod(semester)));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PutMapping("/admin/current-config")
    public ResponseEntity<?> saveCurrentConfig(@RequestAttribute("role") String role,
                                               @RequestBody MonitorSurveyCurrentConfigRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(monitorSurveyService.saveCurrentConfig(request));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/admin/templates")
    public ResponseEntity<?> listTemplates(@RequestAttribute("role") String role) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(monitorSurveyService.listTemplates());
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PostMapping("/admin/templates")
    public ResponseEntity<?> createTemplate(@RequestAttribute("role") String role,
                                            @RequestBody MonitorSurveyTemplateCreateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(monitorSurveyService.createTemplate(request));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PutMapping("/admin/templates/{templateId}")
    public ResponseEntity<?> updateTemplate(@RequestAttribute("role") String role,
                                            @PathVariable Long templateId,
                                            @RequestBody MonitorSurveyTemplateUpdateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(monitorSurveyService.updateTemplate(templateId, request));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @DeleteMapping("/admin/templates/{templateId}")
    public ResponseEntity<?> deleteTemplate(@RequestAttribute("role") String role,
                                            @PathVariable Long templateId) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            monitorSurveyService.deleteTemplate(templateId);
            return ResponseEntity.ok(Map.of("message", "Plantilla eliminada"));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PostMapping("/admin/apply-template")
    public ResponseEntity<?> applyTemplate(@RequestAttribute("role") String role,
                                           @RequestBody MonitorSurveyApplyTemplateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(monitorSurveyService.applyTemplate(request));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/public/questions")
    public ResponseEntity<?> getPublicQuestions(@RequestParam(required = false) String semester) {
        try {
            return ResponseEntity.ok(monitorSurveyService.getPublicQuestions(semester));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/public/current-config")
    public ResponseEntity<?> getPublicCurrentConfig(@RequestParam(required = false) String semester) {
        try {
            return ResponseEntity.ok(monitorSurveyService.getCurrentConfig(semester));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PostMapping("/public/responses")
    public ResponseEntity<?> storePublicResponse(@RequestBody MonitorSurveyPublicResponseRequest request) {
        try {
            monitorSurveyService.storePublicResponse(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Respuesta registrada"));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/public/integration-config")
    public ResponseEntity<?> getIntegrationConfig() {
        try {
            return ResponseEntity.ok(monitorSurveyService.getIntegrationConfig());
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PutMapping("/admin/integration-config")
    public ResponseEntity<?> saveIntegrationConfig(@RequestAttribute("role") String role,
                                                   @RequestBody MonitorSurveyIntegrationConfigRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(monitorSurveyService.saveIntegrationConfig(request));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/admin/report")
    public ResponseEntity<?> getSurveyReport(@RequestAttribute("role") String role,
                                             @RequestParam(required = false) String semester,
                                             @RequestParam(required = false) String monitorCode,
                                             @RequestParam(required = false) String monitoringId) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(monitorSurveyService.getSurveyReport(semester, monitorCode, monitoringId));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/admin/report/csv")
    public ResponseEntity<?> exportSurveyReportCsv(@RequestAttribute("role") String role,
                                                   @RequestParam(required = false) String semester,
                                                   @RequestParam(required = false) String monitorCode,
                                                   @RequestParam(required = false) String monitoringId) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            String csv = monitorSurveyService.exportSurveyReportCsv(semester, monitorCode, monitoringId);
            String filename = "resultados_monitorias" + (semester != null && !semester.isBlank() ? "_" + semester.trim() : "") + ".csv";
            ByteArrayResource resource = new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8));
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(resource);
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    private boolean isDepartmentHead(String role) {
        return role != null && "jfedpto".equalsIgnoreCase(role.trim());
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No está autorizado"));
    }

    private ResponseEntity<Map<String, String>> badRequest(Exception exception) {
        String message = exception.getMessage() == null ? "No se pudo completar la operación" : exception.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    private String normalizeAllowedPeriod(String semester) throws Exception {
        if (semester == null || semester.trim().isEmpty()) {
            throw new Exception("El semestre es obligatorio");
        }

        String normalized = semester.trim();
        if (!normalized.matches("^\\d{4}-[12]$")) {
            throw new Exception("El semestre debe tener formato AAAA-1 o AAAA-2");
        }

        if (Integer.parseInt(normalized.substring(0, 4)) != LocalDate.now().getYear()) {
            throw new Exception("El año del semestre debe corresponder al año actual");
        }

        return normalized;
    }
}
