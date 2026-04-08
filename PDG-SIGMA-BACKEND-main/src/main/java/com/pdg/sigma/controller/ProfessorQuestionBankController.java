package com.pdg.sigma.controller;

import com.pdg.sigma.dto.ProfessorSurveyQuestionCreateRequest;
import com.pdg.sigma.dto.ProfessorSurveyQuestionDTO;
import com.pdg.sigma.dto.ProfessorSurveyQuestionStatusRequest;
import com.pdg.sigma.dto.ProfessorSurveyQuestionUpdateRequest;
import com.pdg.sigma.service.ProfessorSurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RestController
@RequestMapping("/api/preguntas-profesor")
public class ProfessorQuestionBankController {

    @Autowired
    private ProfessorSurveyService professorSurveyService;

    @GetMapping
    public ResponseEntity<?> getAllQuestions(@RequestAttribute("role") String role,
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

    @PostMapping
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

    @PutMapping("/{questionId}")
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

    @PatchMapping("/{questionId}/estado")
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
