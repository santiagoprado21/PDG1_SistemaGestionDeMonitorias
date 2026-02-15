package com.pdg.sigma.dto;

import java.time.LocalTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para reportar conflictos de horarios
 * HU-011: Validación de cruces entre actividades
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConflictDTO {
    
    private Integer activityId;
    private String activityName;
    private String category;
    private Date activityDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String conflictReason;

    /**
     * Constructor simplificado
     */
    public ScheduleConflictDTO(Integer activityId, String activityName, Date activityDate, LocalTime startTime, LocalTime endTime) {
        this.activityId = activityId;
        this.activityName = activityName;
        this.activityDate = activityDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.conflictReason = "Solapamiento de horarios";
    }
}

