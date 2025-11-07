package com.pdg.sigma.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pdg.sigma.dto.CreateRubricRequest;
import com.pdg.sigma.dto.RubricDTO;
import com.pdg.sigma.service.RubricService;

import jakarta.validation.Valid;

/**
 * Controller para Rubric
 * HU-011: Creación de plan de actividades para monitores
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/rubric")
public class RubricController {

    @Autowired
    private RubricService rubricService;

    /**
     * POST /api/rubric/create
     * Crea una nueva rúbrica
     */
    @PostMapping("/create")
    public ResponseEntity<?> createRubric(@Valid @RequestBody CreateRubricRequest request) {
        try {
            RubricDTO rubric = rubricService.createRubric(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(rubric);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    /**
     * PUT /api/rubric/update/{id}
     * Actualiza una rúbrica existente
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateRubric(@PathVariable Long id, @Valid @RequestBody CreateRubricRequest request) {
        try {
            RubricDTO rubric = rubricService.updateRubric(id, request);
            return ResponseEntity.ok(rubric);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/rubric/{id}
     * Obtiene una rúbrica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRubricById(@PathVariable Long id) {
        try {
            RubricDTO rubric = rubricService.getRubricById(id);
            return ResponseEntity.ok(rubric);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/rubric/professor/{professorId}
     * Obtiene todas las rúbricas de un profesor
     */
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<?> getRubricsByProfessor(@PathVariable String professorId) {
        try {
            List<RubricDTO> rubrics = rubricService.getRubricsByProfessor(professorId);
            return ResponseEntity.ok(rubrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/rubric/all
     * Obtiene todas las rúbricas
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllRubrics() {
        try {
            List<RubricDTO> rubrics = rubricService.getAllRubrics();
            return ResponseEntity.ok(rubrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/rubric/search?name=xxx
     * Busca rúbricas por nombre (parcial)
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchRubricsByName(@RequestParam String name) {
        try {
            List<RubricDTO> rubrics = rubricService.searchRubricsByName(name);
            return ResponseEntity.ok(rubrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/rubric/recent
     * Obtiene las rúbricas más recientes (últimas 10)
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentRubrics() {
        try {
            List<RubricDTO> rubrics = rubricService.getRecentRubrics();
            return ResponseEntity.ok(rubrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * DELETE /api/rubric/delete/{id}
     * Elimina una rúbrica
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteRubric(@PathVariable Long id) {
        try {
            rubricService.deleteRubric(id);
            return ResponseEntity.ok("Rúbrica eliminada exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/rubric/exists?name=xxx&professorId=yyy
     * Verifica si existe una rúbrica con un nombre para un profesor
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByNameAndProfessor(
            @RequestParam String name,
            @RequestParam String professorId) {
        boolean exists = rubricService.existsByNameAndProfessor(name, professorId);
        return ResponseEntity.ok(exists);
    }
}

