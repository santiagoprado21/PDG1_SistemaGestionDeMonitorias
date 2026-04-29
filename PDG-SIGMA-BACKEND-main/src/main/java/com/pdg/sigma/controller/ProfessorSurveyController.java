package com.pdg.sigma.controller;

import com.pdg.sigma.dto.*;
import com.pdg.sigma.service.ProfessorSurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RestController
@RequestMapping("/professor-survey")
public class ProfessorSurveyController {

    @Autowired
    private ProfessorSurveyService professorSurveyService;

    @GetMapping("/admin/questions")
    public ResponseEntity<?> getQuestionBank(@RequestAttribute("role") String role,
                                             @RequestParam(required = false) String semester) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            List<ProfessorSurveyQuestionDTO> questions = professorSurveyService.getQuestionBank(semester);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PostMapping("/admin/questions")
    public ResponseEntity<?> createQuestion(@RequestAttribute("role") String role,
                                            @RequestBody ProfessorSurveyQuestionCreateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(professorSurveyService.createQuestion(request));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PutMapping("/admin/questions/{questionId}")
    public ResponseEntity<?> updateQuestion(@RequestAttribute("role") String role,
                                            @PathVariable Long questionId,
                                            @RequestParam(required = false) String semester,
                                            @RequestBody ProfessorSurveyQuestionUpdateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(professorSurveyService.updateQuestion(questionId, request, semester));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PatchMapping("/admin/questions/{questionId}/status")
    public ResponseEntity<?> updateQuestionStatus(@RequestAttribute("role") String role,
                                                  @PathVariable Long questionId,
                                                  @RequestBody ProfessorSurveyQuestionStatusRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            if (request.getBankActive() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Debe indicar el estado de la pregunta"));
            }
            return ResponseEntity.ok(professorSurveyService.updateQuestionStatus(questionId, request.getBankActive()));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/current-config")
    public ResponseEntity<?> getCurrentConfigForMonitor(@RequestParam(required = false) String semester) {
        try {
            return ResponseEntity.ok(professorSurveyService.getCurrentConfig(semester));
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
            return ResponseEntity.ok(professorSurveyService.getCurrentConfig(semester));
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
                                               @RequestBody ProfessorSurveyCurrentConfigRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(professorSurveyService.saveCurrentConfig(request));
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
            return ResponseEntity.ok(professorSurveyService.listTemplates());
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PostMapping("/admin/templates")
    public ResponseEntity<?> createTemplate(@RequestAttribute("role") String role,
                                            @RequestBody ProfessorSurveyTemplateCreateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(professorSurveyService.createTemplate(request));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PutMapping("/admin/templates/{templateId}")
    public ResponseEntity<?> updateTemplate(@RequestAttribute("role") String role,
                                            @PathVariable Long templateId,
                                            @RequestBody ProfessorSurveyTemplateUpdateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(professorSurveyService.updateTemplate(templateId, request));
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
            professorSurveyService.deleteTemplate(templateId);
            return ResponseEntity.ok(Map.of("message", "Plantilla eliminada"));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PostMapping("/admin/apply-template")
    public ResponseEntity<?> applyTemplate(@RequestAttribute("role") String role,
                                           @RequestBody ProfessorSurveyApplyTemplateRequest request) {
        if (!isDepartmentHead(role)) {
            return forbidden();
        }
        try {
            return ResponseEntity.ok(professorSurveyService.applyTemplate(request));
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
            throw new Exception("El periodo es obligatorio");
        }

        String normalized = semester.trim();
        if (!normalized.matches("^\\d{4}-[12]$")) {
            throw new Exception("El periodo debe tener formato AAAA-1 o AAAA-2");
        }

        if (Integer.parseInt(normalized.substring(0, 4)) != LocalDate.now().getYear()) {
            throw new Exception("El año del periodo debe corresponder al año actual");
        }

        return normalized;
    }
}

