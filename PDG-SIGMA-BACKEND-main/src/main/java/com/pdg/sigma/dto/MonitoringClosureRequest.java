package com.pdg.sigma.dto;

import lombok.Data;

/**
 * HU-007: Request para cerrar una monitoría
 */
@Data
public class MonitoringClosureRequest {
    
    /**
     * Comentario opcional del director sobre el cierre
     */
    private String comment;
    
    /**
     * Si es true, el sistema calcula automáticamente las métricas
     * Si es false, se usan los valores proporcionados manualmente
     */
    private Boolean autoCalculate = true;
    
    // Valores manuales (solo si autoCalculate = false)
    private Integer completedActivities;
    private Integer totalActivities;
    private Integer actualHours;
}
