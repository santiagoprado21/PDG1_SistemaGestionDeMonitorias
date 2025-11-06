package com.pdg.sigma.repository;

import com.pdg.sigma.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para MonitorApplication (Postulaciones de Estudiantes a Convocatorias)
 */
@Repository
public interface MonitorApplicationRepository extends JpaRepository<MonitorApplication, Long> {
    
    /**
     * Busca todas las postulaciones a una convocatoria específica
     */
    List<MonitorApplication> findByMonitoringRequest(MonitoringRequest monitoringRequest);
    
    /**
     * Busca todas las postulaciones a una convocatoria por ID
     */
    List<MonitorApplication> findByMonitoringRequestId(Long monitoringRequestId);
    
    /**
     * Busca todas las postulaciones de un monitor (estudiante)
     */
    List<MonitorApplication> findByMonitor(Monitor monitor);
    
    /**
     * Busca todas las postulaciones de un monitor por su ID
     */
    List<MonitorApplication> findByMonitorIdMonitor(String monitorId);
    
    /**
     * Busca postulaciones por convocatoria y estado
     */
    List<MonitorApplication> findByMonitoringRequestAndStatus(MonitoringRequest monitoringRequest, 
                                                              ApplicationStatus status);
    
    /**
     * Busca si un monitor ya se postuló a una convocatoria específica
     */
    Optional<MonitorApplication> findByMonitoringRequestAndMonitor(MonitoringRequest monitoringRequest, 
                                                                    Monitor monitor);
    
    /**
     * Busca si existe postulación por IDs
     */
    @Query("SELECT ma FROM MonitorApplication ma WHERE ma.monitoringRequest.id = :requestId " +
           "AND ma.monitor.code = :monitorCode")
    Optional<MonitorApplication> findByRequestIdAndMonitorCode(@Param("requestId") Long requestId, 
                                                                @Param("monitorCode") String monitorCode);
    
    /**
     * Busca la postulación seleccionada de una convocatoria (solo debe haber una)
     */
    @Query("SELECT ma FROM MonitorApplication ma WHERE ma.monitoringRequest.id = :requestId " +
           "AND ma.status = 'SELECCIONADO'")
    Optional<MonitorApplication> findSelectedApplicationByRequest(@Param("requestId") Long requestId);
    
    /**
     * Cuenta postulaciones a una convocatoria
     */
    Long countByMonitoringRequest(MonitoringRequest monitoringRequest);
    
    /**
     * Cuenta postulaciones por estado en una convocatoria
     */
    Long countByMonitoringRequestAndStatus(MonitoringRequest monitoringRequest, ApplicationStatus status);
    
    /**
     * Busca todas las postulaciones pendientes de un estudiante
     */
    @Query("SELECT ma FROM MonitorApplication ma WHERE ma.monitor.idMonitor = :monitorId " +
           "AND ma.status = 'POSTULADO'")
    List<MonitorApplication> findPendingApplicationsByMonitor(@Param("monitorId") String monitorId);
    
    /**
     * Busca todas las postulaciones seleccionadas de un estudiante
     */
    @Query("SELECT ma FROM MonitorApplication ma WHERE ma.monitor.idMonitor = :monitorId " +
           "AND ma.status = 'SELECCIONADO'")
    List<MonitorApplication> findSelectedApplicationsByMonitor(@Param("monitorId") String monitorId);
    
    /**
     * Busca convocatorias abiertas donde un estudiante aún no se ha postulado
     */
    @Query("SELECT mr FROM MonitoringRequest mr WHERE mr.status = 'CONVOCATORIA_ABIERTA' " +
           "AND mr.program.id = :programId " +
           "AND NOT EXISTS (SELECT ma FROM MonitorApplication ma " +
           "WHERE ma.monitoringRequest = mr AND ma.monitor.idMonitor = :monitorId)")
    List<MonitoringRequest> findAvailableConvocatoriasForMonitor(@Param("monitorId") String monitorId, 
                                                                  @Param("programId") Integer programId);
    
    /**
     * Elimina todas las postulaciones de una convocatoria
     */
    void deleteByMonitoringRequest(MonitoringRequest monitoringRequest);
    
    /**
     * Elimina postulación específica
     */
    void deleteByMonitoringRequestAndMonitor(MonitoringRequest monitoringRequest, Monitor monitor);
}

