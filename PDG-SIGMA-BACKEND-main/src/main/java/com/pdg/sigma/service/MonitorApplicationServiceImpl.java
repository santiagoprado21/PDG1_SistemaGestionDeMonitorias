package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitorApplicationDTO;
import com.pdg.sigma.dto.SelectMonitorRequest;
import com.pdg.sigma.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio para gestionar MonitorApplication (Postulaciones de Estudiantes)
 * Parte del flujo de HU-010
 */
@Service
@Transactional
public class MonitorApplicationServiceImpl implements MonitorApplicationService {

    @Autowired
    private MonitorApplicationRepository monitorApplicationRepository;
    
    @Autowired
    private MonitoringRequestRepository monitoringRequestRepository;
    
    @Autowired
    private MonitorRepository monitorRepository;
    
    @Autowired
    private MonitoringRepository monitoringRepository;
    
    @Autowired
    private MonitoringRequestService monitoringRequestService;
    
    @Autowired
    private ProspectRepository prospectRepository;

    // ==================== MÉTODOS PRINCIPALES ====================

    @Override
    public MonitorApplication applyToConvocatoria(MonitorApplicationDTO dto) throws Exception {
        System.out.println("=== CREANDO POSTULACIÓN A CONVOCATORIA ===");
        
        // 1. Validar que la convocatoria existe
        MonitoringRequest request = monitoringRequestRepository.findById(dto.getMonitoringRequestId())
                .orElseThrow(() -> new Exception("Convocatoria no encontrada con ID: " + dto.getMonitoringRequestId()));
        
        // 2. Validar que la convocatoria esté abierta
        if (!request.isOpenForApplications()) {
            throw new Exception("La convocatoria no está abierta para postulaciones. Estado: " + request.getStatus());
        }
        
        // 3. Buscar o crear el perfil del monitor
        Monitor monitor = getOrCreateMonitor(dto.getMonitorId());
        
        // 4. Validar que no se haya postulado antes
        if (hasApplied(dto.getMonitoringRequestId(), dto.getMonitorId())) {
            throw new Exception("Ya te has postulado a esta convocatoria");
        }
        
        // 5. Validar que cumple los requisitos
        if (!meetsRequirements(dto.getMonitorId(), request)) {
            throw new Exception("No cumples con los requisitos mínimos para esta convocatoria");
        }
        
        // 6. Crear la postulación
        MonitorApplication application = new MonitorApplication(
                request,
                monitor,
                dto.getMotivationLetter()
        );
        
        MonitorApplication saved = monitorApplicationRepository.save(application);
        
        System.out.println("Postulación creada con ID: " + saved.getId());
        System.out.println("Estudiante: " + monitor.getName() + " " + monitor.getLastName());
        System.out.println("Convocatoria: " + request.getCourse().getName());
        System.out.println("==========================================");
        
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorApplication> getApplicationsByRequest(Long requestId) {
        List<MonitorApplication> applications = monitorApplicationRepository.findByMonitoringRequestId(requestId);
        // Forzar carga de relaciones lazy dentro de la transacción
        applications.forEach(app -> {
            if (app.getMonitor() != null) {
                app.getMonitor().getName(); // trigger lazy load
            }
            if (app.getMonitoringRequest() != null) {
                app.getMonitoringRequest().getCourse(); // trigger lazy load
                if (app.getMonitoringRequest().getCourse() != null) {
                    app.getMonitoringRequest().getCourse().getName();
                }
                app.getMonitoringRequest().getProfessor(); // trigger lazy load
                if (app.getMonitoringRequest().getProfessor() != null) {
                    app.getMonitoringRequest().getProfessor().getName();
                }
            }
        });
        return applications;
    }

    @Override
    public List<MonitorApplication> getApplicationsByMonitor(String monitorId) {
        return monitorApplicationRepository.findByMonitorIdMonitor(monitorId);
    }

    @Override
    public void selectMonitor(SelectMonitorRequest request) throws Exception {
        System.out.println("=== SELECCIONANDO MONITOR ===");
        
        // 1. Validar que la postulación existe
        MonitorApplication selectedApplication = monitorApplicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new Exception("Postulación no encontrada"));
        
        // 2. Validar que la convocatoria pertenece al profesor
        MonitoringRequest monitoringRequest = selectedApplication.getMonitoringRequest();
        if (!monitoringRequest.getProfessor().getId().equals(request.getProfessorId())) {
            throw new Exception("Solo el profesor dueño de la convocatoria puede seleccionar monitores");
        }
        
        // 3. Validar que la convocatoria esté abierta
        if (monitoringRequest.getStatus() != RequestStatus.CONVOCATORIA_ABIERTA) {
            throw new Exception("La convocatoria no está abierta. Estado: " + monitoringRequest.getStatus());
        }
        
        // 4. Marcar esta postulación como SELECCIONADO
        selectedApplication.markAsSelected();
        if (request.getNotes() != null) {
            selectedApplication.setNotes(request.getNotes());
        }
        monitorApplicationRepository.save(selectedApplication);
        
        // 5. Marcar las demás postulaciones como NO_SELECCIONADO
        List<MonitorApplication> otherApplications = monitorApplicationRepository
                .findByMonitoringRequest(monitoringRequest);
        
        for (MonitorApplication app : otherApplications) {
            if (!app.getId().equals(selectedApplication.getId()) && 
                app.getStatus() == ApplicationStatus.POSTULADO) {
                app.markAsNotSelected();
                monitorApplicationRepository.save(app);
            }
        }
        
        // 6. Actualizar estado de MonitoringRequest a MONITOR_SELECCIONADO
        monitoringRequestService.markMonitorSelected(monitoringRequest.getId());
        
        // 7. Crear la Monitoring oficial con el monitor asignado
        Monitoring monitoring = new Monitoring(monitoringRequest, selectedApplication.getMonitor());
        Monitoring savedMonitoring = monitoringRepository.save(monitoring);
        
        // 8. Actualizar MonitoringRequest a PENDIENTE_APROBACION
        monitoringRequestService.markPendingApproval(monitoringRequest.getId());
        
        System.out.println("Monitor seleccionado: " + selectedApplication.getMonitor().getName());
        System.out.println("Monitoría creada con ID: " + savedMonitoring.getId());
        System.out.println("Estado: " + savedMonitoring.getApprovalStatus());
        System.out.println("=============================");
    }

    @Override
    public void cancelApplication(Long applicationId, String monitorId) throws Exception {
        MonitorApplication application = monitorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new Exception("Postulación no encontrada"));
        
        // Validar que la postulación pertenece al estudiante
        if (!application.getMonitor().getIdMonitor().equals(monitorId)) {
            throw new Exception("Solo puedes cancelar tus propias postulaciones");
        }
        
        // Solo se puede cancelar si está en estado POSTULADO
        if (application.getStatus() != ApplicationStatus.POSTULADO) {
            throw new Exception("Solo se pueden cancelar postulaciones en estado POSTULADO");
        }
        
        monitorApplicationRepository.delete(application);
        System.out.println("Postulación cancelada: " + applicationId);
    }

    @Override
    public boolean hasApplied(Long requestId, String monitorId) {
        Optional<Monitor> monitor = monitorRepository.findByIdMonitor(monitorId);
        if (monitor.isEmpty()) {
            return false;
        }
        
        Optional<MonitoringRequest> request = monitoringRequestRepository.findById(requestId);
        if (request.isEmpty()) {
            return false;
        }
        
        Optional<MonitorApplication> existing = monitorApplicationRepository
                .findByMonitoringRequestAndMonitor(request.get(), monitor.get());
        
        return existing.isPresent();
    }

    @Override
    public boolean meetsRequirements(String monitorId, MonitoringRequest request) throws Exception {
        // Buscar el prospecto (estudiante) en la tabla prospect
        Optional<Prospect> prospect = prospectRepository.findById(monitorId);
        
        if (prospect.isEmpty()) {
            throw new Exception("Estudiante no encontrado en el sistema");
        }
        
        Prospect student = prospect.get();
        
        // Validar promedio acumulado
        if (request.getRequiredAverageGrade() != null) {
            if (student.getGradeAverage() < request.getRequiredAverageGrade()) {
                System.out.println("No cumple promedio acumulado: " + student.getGradeAverage() + 
                                 " < " + request.getRequiredAverageGrade());
                return false;
            }
        }
        
        // Validar nota del curso
        if (request.getRequiredCourseGrade() != null) {
            if (student.getGradeCourse() < request.getRequiredCourseGrade()) {
                System.out.println("No cumple nota del curso: " + student.getGradeCourse() + 
                                 " < " + request.getRequiredCourseGrade());
                return false;
            }
        }
        
        return true;
    }

    @Override
    public List<MonitoringRequest> getAvailableConvocatoriasForMonitor(String monitorId, Integer programId) {
        return monitorApplicationRepository.findAvailableConvocatoriasForMonitor(monitorId, programId);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Obtiene o crea el perfil de Monitor basado en el Prospect
     */
    private Monitor getOrCreateMonitor(String monitorId) throws Exception {
        // Buscar si ya existe el monitor
        Optional<Monitor> existing = monitorRepository.findByIdMonitor(monitorId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Si no existe, crearlo desde Prospect
        Prospect prospect = prospectRepository.findById(monitorId)
                .orElseThrow(() -> new Exception("Estudiante no encontrado en el sistema"));
        
        Monitor newMonitor = new Monitor(
                prospect.getCode(),
                prospect.getName(),
                prospect.getLastName(),
                prospect.getSemester(),
                prospect.getGradeAverage(),
                prospect.getGradeCourse(),
                prospect.getEmail(),
                prospect.getId()
        );
        
        return monitorRepository.save(newMonitor);
    }

    // ==================== MÉTODOS DE GenericService ====================

    @Override
    public List<MonitorApplication> findAll() {
        return monitorApplicationRepository.findAll();
    }

    @Override
    public Optional<MonitorApplication> findById(Long id) {
        return monitorApplicationRepository.findById(id);
    }

    @Override
    public MonitorApplication save(MonitorApplication entity) throws Exception {
        return monitorApplicationRepository.save(entity);
    }

    @Override
    public MonitorApplication update(MonitorApplication entity) throws Exception {
        if (entity.getId() == null || !monitorApplicationRepository.existsById(entity.getId())) {
            throw new Exception("No se puede actualizar una postulación que no existe");
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return monitorApplicationRepository.save(entity);
    }

    @Override
    public void delete(MonitorApplication entity) throws Exception {
        // Solo se puede eliminar si está en estado POSTULADO o NO_SELECCIONADO
        if (entity.getStatus() == ApplicationStatus.SELECCIONADO) {
            throw new Exception("No se puede eliminar una postulación seleccionada");
        }
        monitorApplicationRepository.delete(entity);
    }

    @Override
    public void deleteById(Long id) throws Exception {
        Optional<MonitorApplication> application = monitorApplicationRepository.findById(id);
        if (application.isEmpty()) {
            throw new Exception("Postulación no encontrada");
        }
        delete(application.get());
    }

    @Override
    public void validate(MonitorApplication entity) throws Exception {
        if (entity.getMonitoringRequest() == null) {
            throw new Exception("La convocatoria es obligatoria");
        }
        if (entity.getMonitor() == null) {
            throw new Exception("El monitor (estudiante) es obligatorio");
        }
    }

    @Override
    public Long count() {
        return monitorApplicationRepository.count();
    }
}

