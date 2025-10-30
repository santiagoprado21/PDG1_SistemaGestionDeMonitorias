package com.pdg.sigma.domain;

/**
 * Estados del ciclo de vida de una MonitoringRequest (Convocatoria de Monitoría)
 */
public enum RequestStatus {
    /**
     * Convocatoria recién creada por el profesor, abierta para que estudiantes se postulen
     */
    CONVOCATORIA_ABIERTA,
    
    /**
     * El profesor ya seleccionó un monitor de los postulantes
     * En este punto se crea automáticamente la Monitoring
     */
    MONITOR_SELECCIONADO,
    
    /**
     * La monitoría fue creada con monitor asignado y está esperando aprobación del jefe de departamento
     */
    PENDIENTE_APROBACION,
    
    /**
     * El jefe de departamento aprobó la monitoría completa (paquete: monitoría + monitor)
     */
    APROBADA,
    
    /**
     * El jefe de departamento rechazó la monitoría
     */
    RECHAZADA,
    
    /**
     * El profesor canceló la convocatoria antes de completar el proceso
     */
    CANCELADA
}

