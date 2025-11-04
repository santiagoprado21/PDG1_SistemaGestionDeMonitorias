package com.pdg.sigma.service;

import com.pdg.sigma.domain.MonitorApplication;
import com.pdg.sigma.domain.MonitoringRequest;
import com.pdg.sigma.dto.MonitorApplicationDTO;
import com.pdg.sigma.dto.SelectMonitorRequest;

import java.util.List;

/**
 * Servicio para gestionar las MonitorApplication (Postulaciones de Estudiantes)
 * Parte del flujo de HU-010
 */
public interface MonitorApplicationService extends GenericService<MonitorApplication, Long> {
    
    /**
     * Crea una nueva postulación de un estudiante a una convocatoria
     * Valida que:
     * - La convocatoria esté abierta
     * - El estudiante no se haya postulado antes
     * - El estudiante cumpla los requisitos
     */
    MonitorApplication applyToConvocatoria(MonitorApplicationDTO dto) throws Exception;
    
    /**
     * Obtiene todas las postulaciones de una convocatoria
     * (Para que el profesor vea los postulantes)
     */
    List<MonitorApplication> getApplicationsByRequest(Long requestId);
    
    /**
     * Obtiene todas las postulaciones de un estudiante
     * (Para que el estudiante vea a dónde se ha postulado)
     */
    List<MonitorApplication> getApplicationsByMonitor(String monitorId);
    
    /**
     * Selecciona un monitor de los postulantes
     * Este método:
     * 1. Marca la postulación seleccionada como SELECCIONADO
     * 2. Marca las demás como NO_SELECCIONADO
     * 3. Crea la Monitoring con el monitor asignado
     * 4. Actualiza los estados de MonitoringRequest
     */
    void selectMonitor(SelectMonitorRequest request) throws Exception;
    
    /**
     * Cancela/elimina una postulación (antes de que se seleccione)
     */
    void cancelApplication(Long applicationId, String monitorId) throws Exception;
    
    /**
     * Verifica si un estudiante ya se postuló a una convocatoria
     */
    boolean hasApplied(Long requestId, String monitorId);
    
    /**
     * Verifica si el estudiante cumple los requisitos de la convocatoria
     */
    boolean meetsRequirements(String monitorId, MonitoringRequest request) throws Exception;
    
    /**
     * Obtiene convocatorias disponibles para un estudiante
     * (Abiertas, de su programa, donde no se ha postulado)
     */
    List<MonitoringRequest> getAvailableConvocatoriasForMonitor(String monitorId, Integer programId);
}

