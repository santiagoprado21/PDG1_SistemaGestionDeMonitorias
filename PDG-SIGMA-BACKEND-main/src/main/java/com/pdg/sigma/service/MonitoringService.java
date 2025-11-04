package com.pdg.sigma.service;

import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.dto.MonitoringDTO;

import java.util.List;

public interface MonitoringService extends GenericService<Monitoring, Long>{
    public Monitoring save(MonitoringDTO monitoringDTO) throws Exception;

    /**
     * Actualiza el presupuesto de una monitoría (horas estimadas y/o valor hora)
     * validando el presupuesto disponible del programa para el semestre.
     * Cualquier parámetro nulo no se actualiza.
     */
    public Monitoring updateMonitoringBudget(Long monitoringId, Integer estimatedHours, Double hourlyRate) throws Exception;

    // ==================== MÉTODOS PARA HU-010 ====================

    /**
     * Aprueba una monitoría (nuevo flujo HU-010)
     * Actualiza el estado de la monitoría y de la MonitoringRequest asociada
     */
    void approveMonitoring(Long monitoringId, String departmentHeadId, String comment) throws Exception;

    /**
     * Rechaza una monitoría (nuevo flujo HU-010)
     * Actualiza el estado de la monitoría y de la MonitoringRequest asociada
     */
    void rejectMonitoring(Long monitoringId, String departmentHeadId, String comment) throws Exception;

    /**
     * Obtiene monitorías pendientes de aprobación
     */
    List<Monitoring> findPendingApproval();

}
