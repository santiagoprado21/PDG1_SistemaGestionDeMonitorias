package com.pdg.sigma.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad Rubric: Representa una rúbrica de evaluación para actividades
 * HU-011: Creación de plan de actividades para monitores
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rubric")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Rubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    /**
     * Criterios de evaluación almacenados como JSON
     * Formato: [{"criterion": "...", "points": 10, "description": "..."}]
     */
    @Column(name = "criteria", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String criteria;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private Professor createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Campo transitorio para trabajar con los criterios como List<RubricCriterion>
     * No se persiste en la BD
     */
    @Transient
    private List<RubricCriterion> criteriaList;

    /**
     * Constructor simplificado sin IDs y timestamps (para creación)
     */
    public Rubric(String name, String description, Integer totalPoints, String criteria, Professor createdBy) {
        this.name = name;
        this.description = description;
        this.totalPoints = totalPoints;
        this.criteria = criteria;
        this.createdBy = createdBy;
    }

    /**
     * Convierte el JSON de criterios a List<RubricCriterion>
     */
    public List<RubricCriterion> getCriteriaList() {
        if (criteriaList == null && criteria != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                criteriaList = mapper.readValue(criteria, new TypeReference<List<RubricCriterion>>() {});
            } catch (Exception e) {
                // Si falla el parsing, retornar lista vacía
                criteriaList = List.of();
            }
        }
        return criteriaList;
    }

    /**
     * Convierte List<RubricCriterion> a JSON y lo asigna al campo criteria
     */
    public void setCriteriaList(List<RubricCriterion> criteriaList) {
        this.criteriaList = criteriaList;
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.criteria = mapper.writeValueAsString(criteriaList);
        } catch (Exception e) {
            this.criteria = "[]";
        }
    }

    /**
     * Clase interna para representar un criterio de la rúbrica
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RubricCriterion {
        private String criterion;
        private Integer points;
        private String description;
    }
}

