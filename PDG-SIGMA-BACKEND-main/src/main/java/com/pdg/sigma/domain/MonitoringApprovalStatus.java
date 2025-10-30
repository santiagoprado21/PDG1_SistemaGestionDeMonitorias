package com.pdg.sigma.domain;

/**
 * Estados de aprobación de una Monitoring (Monitoría Oficial)
 * por parte del Jefe de Departamento
 */
public enum MonitoringApprovalStatus {
    /**
     * Monitoría recién creada con monitor asignado, esperando revisión del jefe de departamento
     */
    PENDIENTE_APROBACION,
    
    /**
     * Jefe de departamento aprobó la monitoría, puede comenzar a funcionar
     */
    APROBADA,
    
    /**
     * Jefe de departamento rechazó la monitoría
     */
    RECHAZADA
}

