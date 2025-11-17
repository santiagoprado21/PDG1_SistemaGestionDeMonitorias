package com.pdg.sigma.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.Rubric;
import com.pdg.sigma.domain.StateActivity;
import com.pdg.sigma.dto.ActivityPlanDTO;
import com.pdg.sigma.dto.ActivityScheduleDTO;
import com.pdg.sigma.dto.ScheduleConflictDTO;
import com.pdg.sigma.repository.ActivityRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.RubricRepository;

/**
 * Implementación del servicio de gestión de horarios
 * HU-011: Creación de plan de actividades para monitores
 */
@Service
public class ActivityScheduleServiceImpl implements ActivityScheduleService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private RubricRepository rubricRepository;

    @Override
    @Transactional
    public ActivityScheduleDTO saveActivityWithSchedule(ActivityScheduleDTO dto) throws Exception {
        // Validar conflictos de horarios (solo si tiene horarios definidos)
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            List<ScheduleConflictDTO> conflicts = validateScheduleConflicts(dto);
            if (!conflicts.isEmpty()) {
                throw new Exception("Conflicto de horarios detectado: " + 
                    conflicts.get(0).getActivityName() + " (" + 
                    conflicts.get(0).getStartTime() + " - " + 
                    conflicts.get(0).getEndTime() + ")");
            }
        }

        // Obtener entidades relacionadas
        Monitoring monitoring = monitoringRepository.findById(Long.valueOf(dto.getMonitoringId()))
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));

        Professor professor = null;
        if (dto.getProfessorId() != null) {
            professor = professorRepository.findById(dto.getProfessorId())
                    .orElseThrow(() -> new Exception("Profesor no encontrado"));
        }

        Monitor monitor = null;
        if (dto.getMonitorId() != null) {
            monitor = monitorRepository.findById(dto.getMonitorId())
                    .orElseThrow(() -> new Exception("Monitor no encontrado"));
        }

        Rubric rubric = null;
        if (dto.getRubricId() != null) {
            rubric = rubricRepository.findById(dto.getRubricId())
                    .orElseThrow(() -> new Exception("Rúbrica no encontrada"));
        }

        // Crear o actualizar la actividad
        Activity activity;
        if (dto.getId() != null) {
            // Actualizar
            activity = activityRepository.findById(dto.getId())
                    .orElseThrow(() -> new Exception("Actividad no encontrada"));
            
            activity.setName(dto.getName());
            activity.setDescription(dto.getDescription());
            activity.setCategory(dto.getCategory());
            activity.setFinish(dto.getFinish());
            activity.setMonitoring(monitoring);
            activity.setProfessor(professor);
            activity.setMonitor(monitor);
            activity.setState(StateActivity.valueOf(dto.getState()));
            activity.setSemester(dto.getSemester());
            activity.setDelivey(dto.getDelivey());
            activity.setEdited(new Date());
            
            // Campos HU-011
            activity.setStartTime(dto.getStartTime());
            activity.setEndTime(dto.getEndTime());
            activity.setDurationHours(dto.getDurationHours());
            activity.setRecurrence(dto.getRecurrence());
            activity.setPriority(dto.getPriority());
            activity.setRubric(rubric);
        } else {
            // Crear nueva
            activity = new Activity(
                dto.getName(),
                dto.getCreation() != null ? dto.getCreation() : new Date(),
                dto.getFinish(),
                dto.getRoleCreator(),
                dto.getRoleResponsable(),
                dto.getCategory(),
                dto.getDescription(),
                monitoring,
                professor,
                monitor,
                StateActivity.valueOf(dto.getState()),
                dto.getDelivey(),
                dto.getSemester(),
                null, // edited
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getDurationHours(),
                dto.getRecurrence(),
                dto.getPriority(),
                rubric
            );
        }

        activity = activityRepository.save(activity);

        return toScheduleDTO(activity);
    }

    @Override
    public List<ScheduleConflictDTO> validateScheduleConflicts(ActivityScheduleDTO dto) throws Exception {
        List<ScheduleConflictDTO> conflicts = new ArrayList<>();

        // Solo validar si hay horarios definidos y un monitor asignado
        if (dto.getStartTime() == null || dto.getEndTime() == null || dto.getMonitorId() == null) {
            return conflicts;
        }

        // Validar que start_time < end_time
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new Exception("La hora de inicio debe ser anterior a la hora de fin");
        }

        // Buscar el monitor
        Monitor monitor = monitorRepository.findById(dto.getMonitorId())
                .orElseThrow(() -> new Exception("Monitor no encontrado"));

        // Buscar conflictos en la BD
        List<Activity> conflictingActivities = activityRepository.findScheduleConflicts(
            monitor,
            dto.getFinish(),
            dto.getStartTime(),
            dto.getEndTime(),
            dto.getId() // Excluir la actividad actual en caso de edición
        );

        // Convertir a DTOs
        conflicts = conflictingActivities.stream()
                .map(a -> new ScheduleConflictDTO(
                    a.getId(),
                    a.getName(),
                    a.getCategory(),
                    a.getFinish(),
                    a.getStartTime(),
                    a.getEndTime(),
                    "Solapamiento de horarios"
                ))
                .collect(Collectors.toList());

        return conflicts;
    }

    @Override
    public ActivityPlanDTO getActivityPlan(Integer monitoringId) throws Exception {
        Monitoring monitoring = monitoringRepository.findById(Long.valueOf(monitoringId))
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));

        List<Activity> activities = activityRepository.findByMonitoringOrderedBySchedule(monitoring);

        // Calcular estadísticas
        int total = activities.size();
        long completed = activities.stream()
                .filter(a -> a.getState() == StateActivity.COMPLETADO || a.getState() == StateActivity.COMPLETADOT)
                .count();
        int pending = total - (int) completed;

        // Calcular total de horas
        double totalHours = activities.stream()
                .filter(a -> a.getDurationHours() != null)
                .mapToDouble(a -> a.getDurationHours().doubleValue())
                .sum();

        // Convertir a DTOs
        List<ActivityScheduleDTO> activityDTOs = activities.stream()
                .map(this::toScheduleDTO)
                .collect(Collectors.toList());

        // Construir el DTO del plan
        ActivityPlanDTO plan = new ActivityPlanDTO();
        plan.setMonitoringId(monitoring.getId().intValue());
        plan.setCourseName(monitoring.getCourse() != null ? monitoring.getCourse().getName() : "N/A");
        plan.setProgramName(monitoring.getProgram() != null ? monitoring.getProgram().getName() : "N/A");
        plan.setProfessorName(monitoring.getProfessor() != null ? monitoring.getProfessor().getName() : "N/A");
        
        // Obtener nombre del monitor - Compatible con ambos flujos (nuevo y antiguo)
        String monitorName = "Sin asignar";
        
        // Flujo NUEVO (HU-010): Monitor asignado directamente
        if (monitoring.getAssignedMonitor() != null) {
            monitorName = monitoring.getAssignedMonitor().getName() + " " + 
                         monitoring.getAssignedMonitor().getLastName();
        } 
        // Flujo ANTIGUO: Monitor en la tabla de relación monitoring_monitor
        else if (monitoring.getMonitoringMonitors() != null && !monitoring.getMonitoringMonitors().isEmpty()) {
            // Priorizar "seleccionado" sobre "aprobado"
            var selectedMonitor = monitoring.getMonitoringMonitors().stream()
                .filter(mm -> "seleccionado".equalsIgnoreCase(mm.getEstadoSeleccion()))
                .findFirst();
            
            // Si no hay "seleccionado", buscar "aprobado"
            if (selectedMonitor.isEmpty()) {
                selectedMonitor = monitoring.getMonitoringMonitors().stream()
                    .filter(mm -> "aprobado".equalsIgnoreCase(mm.getEstadoSeleccion()))
                    .findFirst();
            }
            
            // Asignar el nombre del monitor encontrado
            if (selectedMonitor.isPresent() && selectedMonitor.get().getMonitor() != null) {
                monitorName = selectedMonitor.get().getMonitor().getName() + " " + 
                             selectedMonitor.get().getMonitor().getLastName();
            }
        }
        
        plan.setMonitorName(monitorName.trim());
        
        plan.setSemester(monitoring.getSemester());
        plan.setTotalActivities(total);
        plan.setCompletedActivities((int) completed);
        plan.setPendingActivities(pending);
        plan.setActivities(activityDTOs);
        plan.setTotalHours(totalHours);

        return plan;
    }

    @Override
    public List<ActivityScheduleDTO> getMonitorSchedule(String monitorId, Date startDate, Date endDate) throws Exception {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new Exception("Monitor no encontrado"));

        List<Activity> activities = activityRepository.findByMonitorAndDateRange(monitor, startDate, endDate);

        return activities.stream()
                .map(this::toScheduleDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActivityScheduleDTO> getProfessorSchedule(String professorId, Date startDate, Date endDate) throws Exception {
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new Exception("Profesor no encontrado"));

        List<Activity> activities = activityRepository.findByProfessorAndDateRange(professor, startDate, endDate);

        return activities.stream()
                .map(this::toScheduleDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ActivityScheduleDTO toScheduleDTO(Activity activity) {
        ActivityScheduleDTO dto = new ActivityScheduleDTO();
        
        // Campos base
        dto.setId(activity.getId());
        dto.setName(activity.getName());
        dto.setDescription(activity.getDescription());
        dto.setCategory(activity.getCategory());
        dto.setCreation(activity.getCreation());
        dto.setFinish(activity.getFinish());
        dto.setRoleCreator(activity.getRoleCreator());
        dto.setRoleResponsable(activity.getRoleResponsable());
        dto.setState(activity.getState().name());
        dto.setSemester(activity.getSemester());
        dto.setDelivey(activity.getDelivey());

        // IDs de relaciones
        if (activity.getMonitoring() != null) {
            dto.setMonitoringId(activity.getMonitoring().getId().intValue());
        }
        if (activity.getProfessor() != null) {
            dto.setProfessorId(activity.getProfessor().getId());
        }
        if (activity.getMonitor() != null) {
            dto.setMonitorId(activity.getMonitor().getIdMonitor());
        }

        // Campos HU-011
        dto.setStartTime(activity.getStartTime());
        dto.setEndTime(activity.getEndTime());
        dto.setDurationHours(activity.getDurationHours());
        dto.setRecurrence(activity.getRecurrence());
        dto.setPriority(activity.getPriority());

        // Información de rúbrica
        if (activity.getRubric() != null) {
            dto.setRubricId(activity.getRubric().getId());
            dto.setRubricName(activity.getRubric().getName());
            dto.setRubricTotalPoints(activity.getRubric().getTotalPoints());
        }

        return dto;
    }
}

