package com.pdg.sigma.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el plan completo de actividades de una monitoría
 * HU-011: Creación de plan de actividades para monitores
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPlanDTO {
    
    private Integer monitoringId;
    private String courseName;
    private String programName;
    private String professorName;
    private String monitorName;
    private String semester;
    private Integer totalActivities;
    private Integer completedActivities;
    private Integer pendingActivities;
    private List<ActivityScheduleDTO> activities;
    private Double totalHours; // Suma de duration_hours de todas las actividades
}

