package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO simplificado para listar convocatorias abiertas
 * (Vista de estudiantes)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvocatoriaListDTO implements Serializable {
    
    private Long id;
    private String courseName;
    private String professorName;
    private String programName;
    private Integer requestedHours;
    private String semester;
    private Date startDate;
    private Date finishDate;
    
    // Requisitos
    private Double requiredAverageGrade;
    private Double requiredCourseGrade;
    
    // Info adicional
    private Integer applicationCount;
    private Boolean alreadyApplied; // true si el estudiante ya se postuló
    private Boolean meetsRequirements; // true si cumple los requisitos
}

