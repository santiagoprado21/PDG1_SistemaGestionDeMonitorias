package com.pdg.sigma.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.pdg.sigma.domain.Rubric.RubricCriterion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para Rubric
 * HU-011: Creación de plan de actividades para monitores
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RubricDTO {
    
    private Long id;
    private String name;
    private String description;
    private Integer totalPoints;
    private List<RubricCriterion> criteria;
    private String createdBy; // ID del profesor
    private String createdByName; // Nombre del profesor
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Constructor simplificado para creación
     */
    public RubricDTO(Long id, String name, String description, Integer totalPoints, List<RubricCriterion> criteria) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.totalPoints = totalPoints;
        this.criteria = criteria;
    }
}

