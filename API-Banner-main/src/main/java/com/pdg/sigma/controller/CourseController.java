package com.pdg.sigma.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pdg.sigma.domain.Course;
import com.pdg.sigma.domain.CourseProfessor;
import com.pdg.sigma.repository.CourseProfessorRepository;
import com.pdg.sigma.service.CourseServiceImpl;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:5433") 
public class CourseController {

    
    @Autowired
    CourseServiceImpl courseService;
    
    @Autowired
    CourseProfessorRepository courseProfessorRepository; 
    

    @GetMapping("/byProfessor/{professorId}")
    public ResponseEntity<List<Course>> getCoursesByProfessor(@PathVariable String professorId) {
        
        List<CourseProfessor> relations = courseProfessorRepository.findByProfessor(professorId);
        
        if (relations.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Course> courses = relations.stream()
                                        .map(CourseProfessor::getCourse)
                                        .collect(Collectors.toList());

        return ResponseEntity.ok(courses);
    }
}
