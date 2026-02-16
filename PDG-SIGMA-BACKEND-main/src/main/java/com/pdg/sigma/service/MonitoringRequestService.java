package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitoringRequestDTO;

import java.util.List;

/**
 * Servicio para gestionar las MonitoringRequest (Convocatorias de Monitoría)
 * HU-010: Crear postulación de monitorias por parte de los profesores
 */
public interface MonitoringRequestService extends GenericService<MonitoringRequest, Long> {
    
    /**
     * Crea una nueva convocatoria de monitoría
     * Valida permisos del profesor, presupuesto disponible y duplicados
     */
    MonitoringRequest createConvocatoria(MonitoringRequestDTO dto) throws Exception;
    
    /**
     * Obtiene todas las convocatorias abiertas (CONVOCATORIA_ABIERTA)
     * para que los estudiantes puedan ver y postularse
     */
    List<MonitoringRequest> findOpenConvocatorias();
    
    /**
     * Obtiene convocatorias abiertas filtradas por programa
     * (Para que estudiantes vean solo las de su programa)
     */
    List<MonitoringRequest> findOpenConvocatoriasByProgram(Integer programId);
    
    /**
     * Obtiene todas las convocatorias de un profesor
     */
    List<MonitoringRequest> findByProfessor(String professorId);
    
    /**
     * Obtiene convocatorias pendientes de aprobación para el jefe de departamento
     * Filtra por programa/departamento del jefe
     */
    List<MonitoringRequest> findPendingApprovalForDepartmentHead(String departmentHeadId) throws Exception;
    
    /**
     * Cancela una convocatoria (solo si está en estado CONVOCATORIA_ABIERTA)
     * El profesor puede cancelarla antes de seleccionar monitor
     */
    void cancelConvocatoria(Long requestId, String professorId) throws Exception;
    
    /**
     * Cambia el estado de la convocatoria a MONITOR_SELECCIONADO
     * Se llama desde MonitorApplicationService cuando se selecciona un monitor
     */
    void markMonitorSelected(Long requestId) throws Exception;
    
    /**
     * Cambia el estado a PENDIENTE_APROBACION
     * Se llama cuando se crea la Monitoring asociada
     */
    void markPendingApproval(Long requestId) throws Exception;
    
    /**
     * Marca la convocatoria como APROBADA
     * Se llama cuando el jefe de departamento aprueba la Monitoring
     */
    void markApproved(Long requestId) throws Exception;
    
    /**
     * Marca la convocatoria como RECHAZADA
     * Se llama cuando el jefe de departamento rechaza la Monitoring
     */
    void markRejected(Long requestId) throws Exception;
    
    /**
     * Valida que el profesor tenga permiso para crear convocatoria en ese curso
     */
    boolean validateProfessorPermission(String professorId, Long courseId) throws Exception;
    
    /**
     * Valida que haya presupuesto disponible para las horas solicitadas
     */
    boolean validateBudgetAvailability(Long programId, String semester, Integer requestedHours) throws Exception;
    
    /**
     * Obtiene el número de postulantes de una convocatoria
     */
    Integer getApplicationCount(Long requestId);
    
    // ==================== NUEVOS MÉTODOS: APROBACIÓN DEL JEFE AL INICIO ====================
    
    /**
     * Obtiene convocatorias pendientes de aprobación inicial del jefe
     * (estado PENDIENTE_APROBACION_JEFE)
     */
    List<MonitoringRequest> findPendingHeadApproval(String departmentHeadId) throws Exception;
    
    /**
     * El jefe aprueba una convocatoria (cambia a CONVOCATORIA_ABIERTA)
     * Permite que los estudiantes se postulen
     */
    void approveByHead(Long requestId, String departmentHeadId, String comment) throws Exception;
    
    /**
     * El jefe rechaza una convocatoria
     */
    void rejectByHead(Long requestId, String departmentHeadId, String comment) throws Exception;
    
    /**
     * El jefe modifica y aprueba una convocatoria en un solo paso
     * Puede cambiar horas, fechas, justificación, etc.
     */
    MonitoringRequest modifyAndApproveByHead(Long requestId, MonitoringRequestDTO modifications, 
                                            String departmentHeadId, String comment) throws Exception;
}

