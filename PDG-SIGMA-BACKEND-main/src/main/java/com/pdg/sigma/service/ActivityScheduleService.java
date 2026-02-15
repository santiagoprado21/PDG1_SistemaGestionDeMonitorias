package com.pdg.sigma.service;

import java.util.Date;
import java.util.List;

import com.pdg.sigma.dto.ActivityPlanDTO;
import com.pdg.sigma.dto.ActivityScheduleDTO;
import com.pdg.sigma.dto.ScheduleConflictDTO;

/**
 * Service interface para gestión de horarios y planes de actividades
 * HU-011: Creación de plan de actividades para monitores
 */
public interface ActivityScheduleService {

    /**
     * Crea o actualiza una actividad con horarios y valida conflictos
     * @return ActivityScheduleDTO con la actividad creada/actualizada
     * @throws Exception si hay conflictos de horarios o errores de validación
     */
    ActivityScheduleDTO saveActivityWithSchedule(ActivityScheduleDTO dto) throws Exception;

    /**
     * Valida si hay conflictos de horarios para una actividad
     * @return Lista de conflictos encontrados (vacía si no hay conflictos)
     */
    List<ScheduleConflictDTO> validateScheduleConflicts(ActivityScheduleDTO dto) throws Exception;

    /**
     * Obtiene el plan completo de actividades para una monitoría
     */
    ActivityPlanDTO getActivityPlan(Integer monitoringId) throws Exception;

    /**
     * Obtiene actividades con horarios de un monitor en un rango de fechas
     */
    List<ActivityScheduleDTO> getMonitorSchedule(String monitorId, Date startDate, Date endDate) throws Exception;

    /**
     * Obtiene actividades con horarios de un profesor en un rango de fechas
     */
    List<ActivityScheduleDTO> getProfessorSchedule(String professorId, Date startDate, Date endDate) throws Exception;

    /**
     * Convierte una Activity entity a ActivityScheduleDTO
     */
    ActivityScheduleDTO toScheduleDTO(com.pdg.sigma.domain.Activity activity);

    /**
     * HU-017: Obtiene todos los planes de actividades de un monitor
     * @param monitorId Código del monitor
     * @return Lista de planes de actividades de todas las monitorías asignadas
     */
    List<ActivityPlanDTO> getMonitorActivityPlans(String monitorId) throws Exception;
}

