package com.pdg.sigma.domain;

/**
 * Estados de una postulación de estudiante (MonitorApplication)
 */
public enum ApplicationStatus {
    /**
     * El estudiante se postuló a la convocatoria
     */
    POSTULADO,
    
    /**
     * El profesor eligió este estudiante como monitor
     * Solo una MonitorApplication puede tener este estado por MonitoringRequest
     */
    SELECCIONADO,
    
    /**
     * Los demás estudiantes que no fueron seleccionados
     */
    NO_SELECCIONADO
}

