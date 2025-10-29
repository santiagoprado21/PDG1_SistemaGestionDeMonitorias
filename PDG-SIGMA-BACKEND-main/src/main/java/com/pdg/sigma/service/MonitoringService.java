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


}
