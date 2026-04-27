package com.pdg.sigma.controller;

import com.pdg.sigma.dto.MonitorSurveyQuestionCreateRequest;
import com.pdg.sigma.dto.MonitorSurveyQuestionDTO;
import com.pdg.sigma.dto.MonitorSurveyQuestionUpdateRequest;
import com.pdg.sigma.service.MonitorSurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RestController
@RequestMapping("/api/preguntas-monitor")
public class MonitorQuestionBankController {

    @Autowired
    private MonitorSurveyService monitorSurveyService;

    @GetMapping
    public ResponseEntity<?> getAllQuestions(@RequestAttribute("role") String role,
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

    @PostMapping
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

    @PutMapping("/{questionId}")
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
}
