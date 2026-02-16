package com.pdg.sigma.service;

import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.dto.MonitoringClosureRequest;
import com.pdg.sigma.dto.MonitoringClosureReport;

import java.util.List;

/**
 * HU-007: Servicio para cierre de monitorías al final del semestre
 */
public interface MonitoringClosureService {
    
    /**
     * Obtiene todas las monitorías que pueden ser cerradas (estado APROBADA)
     * @param semester Semestre a filtrar
     * @param programId ID del programa (opcional)
     * @return Lista de monitorías aprobadas
     */
    List<Monitoring> getMonitoringsReadyForClosure(String semester, Integer programId);
    
    /**
     * Cierra una monitoría individual
     * @param monitoringId ID de la monitoría
     * @param request Datos del cierre
     * @param directorId ID del director que cierra
     * @return Reporte de cumplimiento generado
     * @throws Exception Si la monitoría no puede ser cerrada
     */
    MonitoringClosureReport closeMonitoring(Long monitoringId, MonitoringClosureRequest request, String directorId) throws Exception;
    
    /**
     * Cierra múltiples monitorías en lote
     * @param monitoringIds Lista de IDs de monitorías
     * @param directorId ID del director que cierra
     * @param request Datos del cierre (comentario, autoCalculate)
     * @return Lista de reportes generados
     * @throws Exception Si alguna monitoría no puede ser cerrada
     */
    List<MonitoringClosureReport> closeMonitoringsBatch(List<Long> monitoringIds, String directorId, MonitoringClosureRequest request) throws Exception;
    
    /**
     * Genera reporte de cumplimiento para una monitoría cerrada
     * @param monitoringId ID de la monitoría
     * @return Reporte de cumplimiento
     * @throws Exception Si la monitoría no existe o no está cerrada
     */
    MonitoringClosureReport generateComplianceReport(Long monitoringId) throws Exception;
    
    /**
     * Obtiene todas las monitorías cerradas de un semestre
     * @param semester Semestre
     * @param programId ID del programa (opcional)
     * @return Lista de monitorías cerradas
     */
    List<Monitoring> getClosedMonitorings(String semester, Integer programId);
}
