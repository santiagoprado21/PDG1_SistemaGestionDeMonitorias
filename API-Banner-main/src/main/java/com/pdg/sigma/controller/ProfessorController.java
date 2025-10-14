package com.pdg.sigma.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.service.ProfessorServiceImpl;

@RestController
@RequestMapping("/api/professors")
@CrossOrigin(origins = "http://localhost:5433")
public class ProfessorController {

    @Autowired
    ProfessorServiceImpl professorService;

    @GetMapping("/{id}")
    public ResponseEntity<Professor> getProfessorById(@PathVariable String id) {
        Optional<Professor> professorOptional = professorService.findById(id);
        if (professorOptional.isPresent()) {
            return ResponseEntity.ok(professorOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}