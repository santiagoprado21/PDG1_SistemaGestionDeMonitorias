package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para solicitar la aprobación o rechazo de una Monitoring
 * por parte del Jefe de Departamento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveMonitoringRequest implements Serializable {
    
    /**
     * ID de la monitoría a aprobar/rechazar
     */
    private Long monitoringId;
    
    /**
     * ID del jefe de departamento que toma la decisión
     */
    private String departmentHeadId;
    
    /**
     * Comentario sobre la decisión tomada
     */
    private String comment;
    
    /**
     * true para aprobar, false para rechazar
     */
    private Boolean approved;
}

