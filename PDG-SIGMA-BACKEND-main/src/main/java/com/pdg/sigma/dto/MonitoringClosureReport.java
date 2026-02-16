package com.pdg.sigma.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * HU-007: Reporte de cumplimiento generado al cerrar una monitoría
 */
@Data
public class MonitoringClosureReport {
    
    // Información básica
    private Long monitoringId;
    private String courseName;
    private String programName;
    private String semester;
    private String monitorName;
    private String professorName;
    
    // Fechas
    private LocalDateTime startDate;
    private LocalDateTime finishDate;
    private LocalDateTime closureDate;
    
    // Métricas de cumplimiento
    private Integer compliancePercentage;
    private Integer completedActivities;
    private Integer totalActivities;
    private Integer actualHours;
    private Integer estimatedHours;
    
    // Presupuesto
    private Double hourlyRate;
    private Double totalBudgetUsed;
    
    // Auditoría
    private String closedBy;
    private String closureComment;
    private String approvedBy;
    
    // Estado
    private String status;
    
    public MonitoringClosureReport() {
    }
}
