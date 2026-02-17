package com.pdg.sigma.service;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.MonitoringApprovalStatus;
import com.pdg.sigma.domain.StateActivity;
import com.pdg.sigma.dto.MonitoringClosureReport;
import com.pdg.sigma.dto.MonitoringClosureRequest;
import com.pdg.sigma.repository.ActivityRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HU-007: Implementación del servicio de cierre de monitorías
 */
@Service
public class MonitoringClosureServiceImpl implements MonitoringClosureService {

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Monitoring> getMonitoringsReadyForClosure(String semester, Integer programId) {
        List<Monitoring> monitorings;
        
        if (programId != null) {
            monitorings = monitoringRepository.findBySemesterAndProgramIdAndApprovalStatus(
                semester, programId, MonitoringApprovalStatus.APROBADA
            );
        } else {
            monitorings = monitoringRepository.findBySemesterAndApprovalStatus(
                semester, MonitoringApprovalStatus.APROBADA
            );
        }
        
        // Forzar carga de relaciones lazy
        monitorings.forEach(m -> {
            if (m.getCourse() != null) {
                m.getCourse().getName();
            }
            if (m.getProgram() != null) {
                m.getProgram().getName();
            }
            if (m.getProfessor() != null) {
                m.getProfessor().getName();
            }
            if (m.getAssignedMonitor() != null) {
                m.getAssignedMonitor().getName();
            }
        });
        
        return monitorings;
    }

    @Override
    @Transactional
    public MonitoringClosureReport closeMonitoring(Long monitoringId, MonitoringClosureRequest request, String directorId) throws Exception {
        System.out.println("=== CERRANDO MONITORÍA ===");
        System.out.println("ID: " + monitoringId);
        System.out.println("Director: " + directorId);
        
        // 1. Obtener la monitoría
        Monitoring monitoring = monitoringRepository.findById(monitoringId)
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));
        
        // 2. Validar que puede ser cerrada
        if (!monitoring.canBeClosed()) {
            throw new Exception("La monitoría no puede ser cerrada. Estado actual: " + monitoring.getApprovalStatus());
        }
        
        // 3. Calcular o usar métricas proporcionadas
        Integer completedActivities;
        Integer totalActivities;
        Integer actualHours;
        
        if (request.getAutoCalculate()) {
            // Calcular automáticamente desde las actividades
            List<Activity> activities = activityRepository.findByMonitoringId(monitoringId);
            
            totalActivities = activities.size();
            completedActivities = (int) activities.stream()
                    .filter(a -> a.getState() == StateActivity.COMPLETADO || a.getState() == StateActivity.COMPLETADOT)
                    .count();
            
            // Calcular horas reales (usar horas estimadas de la monitoría como base)
            actualHours = monitoring.getEstimatedHours() != null ? monitoring.getEstimatedHours() : 0;
            
            System.out.println("Métricas calculadas automáticamente:");
            System.out.println("- Total actividades: " + totalActivities);
            System.out.println("- Completadas: " + completedActivities);
            System.out.println("- Horas reales: " + actualHours);
        } else {
            // Usar valores manuales
            completedActivities = request.getCompletedActivities();
            totalActivities = request.getTotalActivities();
            actualHours = request.getActualHours();
            
            System.out.println("Usando métricas manuales:");
            System.out.println("- Total actividades: " + totalActivities);
            System.out.println("- Completadas: " + completedActivities);
            System.out.println("- Horas reales: " + actualHours);
        }
        
        // 4. Cerrar la monitoría
        monitoring.close(directorId, request.getComment(), completedActivities, totalActivities, actualHours);
        
        // 5. Guardar
        Monitoring closedMonitoring = monitoringRepository.save(monitoring);
        
        System.out.println("Monitoría cerrada exitosamente");
        System.out.println("Cumplimiento: " + closedMonitoring.getCompliancePercentage() + "%");
        System.out.println("=============================");
        
        // 6. Generar reporte
        return generateComplianceReport(closedMonitoring.getId());
    }

    @Override
    @Transactional
    public List<MonitoringClosureReport> closeMonitoringsBatch(List<Long> monitoringIds, String directorId, MonitoringClosureRequest request) throws Exception {
        System.out.println("=== CIERRE EN LOTE ===");
        System.out.println("Total monitorías: " + monitoringIds.size());
        System.out.println("Director: " + directorId);
        System.out.println("Comentario: " + request.getComment());
        
        List<MonitoringClosureReport> reports = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (Long monitoringId : monitoringIds) {
            try {
                MonitoringClosureReport report = closeMonitoring(monitoringId, request, directorId);
                reports.add(report);
                
            } catch (Exception e) {
                String error = "Monitoría ID " + monitoringId + ": " + e.getMessage();
                errors.add(error);
                System.err.println("Error: " + error);
            }
        }
        
        System.out.println("Cerradas exitosamente: " + reports.size());
        System.out.println("Errores: " + errors.size());
        System.out.println("=====================");
        
        if (!errors.isEmpty()) {
            throw new Exception("Algunas monitorías no pudieron cerrarse: " + String.join("; ", errors));
        }
        
        return reports;
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringClosureReport generateComplianceReport(Long monitoringId) throws Exception {
        Monitoring monitoring = monitoringRepository.findById(monitoringId)
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));
        
        MonitoringClosureReport report = new MonitoringClosureReport();
        
        // Información básica
        report.setMonitoringId(monitoring.getId());
        report.setCourseName(monitoring.getCourse() != null ? monitoring.getCourse().getName() : "N/A");
        report.setProgramName(monitoring.getProgram() != null ? monitoring.getProgram().getName() : "N/A");
        report.setSemester(monitoring.getSemester());
        
        // Monitor y profesor
        if (monitoring.getAssignedMonitor() != null) {
            report.setMonitorName(monitoring.getAssignedMonitor().getName() + " " + monitoring.getAssignedMonitor().getLastName());
        } else if (monitoring.getMonitoringMonitors() != null && !monitoring.getMonitoringMonitors().isEmpty()) {
            // Flujo antiguo con múltiples monitores
            String monitors = monitoring.getMonitoringMonitors().stream()
                    .map(mm -> mm.getMonitor().getName())
                    .collect(Collectors.joining(", "));
            report.setMonitorName(monitors);
        } else {
            report.setMonitorName("Sin monitor asignado");
        }
        
        if (monitoring.getProfessor() != null) {
            report.setProfessorName(monitoring.getProfessor().getName());
        }
        
        // Fechas
        if (monitoring.getStart() != null) {
            report.setStartDate(monitoring.getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (monitoring.getFinish() != null) {
            report.setFinishDate(monitoring.getFinish().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        report.setClosureDate(monitoring.getClosureDate());
        
        // Métricas
        report.setCompliancePercentage(monitoring.getCompliancePercentage());
        report.setCompletedActivities(monitoring.getCompletedActivities());
        report.setTotalActivities(monitoring.getTotalActivities());
        report.setActualHours(monitoring.getActualHours());
        report.setEstimatedHours(monitoring.getEstimatedHours());
        
        // Presupuesto
        report.setHourlyRate(monitoring.getHourlyRate());
        if (monitoring.getActualHours() != null && monitoring.getHourlyRate() != null) {
            report.setTotalBudgetUsed(monitoring.getActualHours() * monitoring.getHourlyRate());
        }
        
        // Auditoría
        report.setClosedBy(monitoring.getClosedBy());
        report.setClosureComment(monitoring.getClosureComment());
        report.setApprovedBy(monitoring.getApprovedBy());
        report.setStatus(monitoring.getApprovalStatus().toString());
        
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Monitoring> getClosedMonitorings(String semester, Integer programId) {
        List<Monitoring> monitorings;
        
        if (programId != null) {
            monitorings = monitoringRepository.findBySemesterAndProgramIdAndApprovalStatus(
                semester, programId, MonitoringApprovalStatus.CERRADA
            );
        } else {
            monitorings = monitoringRepository.findBySemesterAndApprovalStatus(
                semester, MonitoringApprovalStatus.CERRADA
            );
        }
        
        // Forzar carga de relaciones lazy
        monitorings.forEach(m -> {
            if (m.getCourse() != null) {
                m.getCourse().getName();
            }
            if (m.getProgram() != null) {
                m.getProgram().getName();
            }
            if (m.getProfessor() != null) {
                m.getProfessor().getName();
            }
            if (m.getAssignedMonitor() != null) {
                m.getAssignedMonitor().getName();
            }
        });
        
        return monitorings;
    }
}
