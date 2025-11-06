package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para que el profesor seleccione un monitor de los postulantes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectMonitorRequest implements Serializable {
    
    /**
     * ID de la convocatoria (MonitoringRequest)
     */
    private Long monitoringRequestId;
    
    /**
     * ID de la postulación (MonitorApplication) seleccionada
     */
    private Long applicationId;
    
    /**
     * ID del profesor que selecciona (para validación)
     */
    private String professorId;
    
    /**
     * Comentarios opcionales sobre la selección
     */
    private String notes;
}

