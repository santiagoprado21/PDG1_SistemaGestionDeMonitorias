package com.pdg.sigma.dto;

import java.util.List;

import com.pdg.sigma.domain.Rubric.RubricCriterion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una nueva rúbrica
 * HU-011: Creación de plan de actividades para monitores
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRubricRequest {

    @NotNull(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;

    private String description;

    @NotNull(message = "El total de puntos es obligatorio")
    @Min(value = 1, message = "El total de puntos debe ser mayor a 0")
    private Integer totalPoints;

    @NotEmpty(message = "Debe incluir al menos un criterio")
    private List<RubricCriterion> criteria;

    @NotNull(message = "El ID del profesor es obligatorio")
    private String professorId;
}

