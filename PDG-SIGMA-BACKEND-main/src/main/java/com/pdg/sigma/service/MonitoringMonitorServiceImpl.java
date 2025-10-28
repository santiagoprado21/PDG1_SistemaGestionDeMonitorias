package com.pdg.sigma.service;

import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.dto.MonitorDTO;
import com.pdg.sigma.dto.ApproveApplicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pdg.sigma.repository.MonitoringMonitorRepository;

import jakarta.persistence.EntityNotFoundException;

import com.pdg.sigma.domain.MonitoringMonitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MonitoringMonitorServiceImpl {

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    // public List<MonitorDTO> getMonitorsByMonitoringId(Long monitoringId) {
    //     List<MonitoringMonitor> monitoringMonitors = monitoringMonitorRepository.findByMonitoringId(monitoringId);
    //     List<MonitorDTO> monitors = new ArrayList<>();
    //     Professor professor = monitoringMonitors.get(0).getMonitoring().getProfessor();
    //     monitors.add(new MonitorDTO(professor.getName()+" "+professor.getId(), professor.getId(), "P"));

    //     for(MonitoringMonitor monitoringMonitor: monitoringMonitors){
    //         String name = monitoringMonitor.getMonitor().getName()+" "+monitoringMonitor.getMonitor().getLastName()+" "+monitoringMonitor.getMonitor().getCode();
    //         String id = monitoringMonitor.getMonitor().getIdMonitor();
    //         monitors.add(new MonitorDTO(name,id,"M"));
    //     }
    //     return monitors;
    // }

    @Override
    public List<MonitorDTO> getMonitorsByMonitoringId(Long monitoringId) {
        List<MonitoringMonitor> monitoringMonitors = monitoringMonitorRepository.findByMonitoringId(monitoringId);
        List<MonitorDTO> monitors = new ArrayList<>();

        Professor professor = monitoringMonitors.get(0).getMonitoring().getProfessor();
        monitors.add(new MonitorDTO(professor.getName() + " " + professor.getId(), professor.getId(), "P")); 

        for (MonitoringMonitor monitoringMonitor : monitoringMonitors) {
            String name = monitoringMonitor.getMonitor().getName() + " " +
                        monitoringMonitor.getMonitor().getLastName() + " " +
                        monitoringMonitor.getMonitor().getCode();
            String id = monitoringMonitor.getMonitor().getIdMonitor();

            monitors.add(new MonitorDTO(name, id, "M"));
        }

        return monitors;
    }

    @Override
    public void deleteRelation(Long idMonitoring, String monitorCode) throws Exception {
        monitoringMonitorRepository.deleteByMonitoringIdAndMonitor_Code(idMonitoring, monitorCode.trim());
    }

    @Override
    public void updateApplicantSelectionStatus(Long monitoringId, String monitorCode, String newStatus) {
    MonitoringMonitor mm = monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(monitoringId, monitorCode)
        .orElseThrow(() -> new EntityNotFoundException("MonitoringMonitor relation not found for monitoringId " + monitoringId + " and monitor code " + monitorCode));

    mm.setEstadoSeleccion(newStatus);
    monitoringMonitorRepository.save(mm);
}

    @Override
    public void approveApplication(ApproveApplicationRequest request) throws Exception {
        System.out.println("=== APROBANDO POSTULACIÓN ===");
        System.out.println("MonitoringId: " + request.getMonitoringId());
        System.out.println("MonitorCode: " + request.getMonitorCode());
        
        MonitoringMonitor mm = monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(
            request.getMonitoringId(), 
            request.getMonitorCode()
        ).orElseThrow(() -> new EntityNotFoundException(
            "MonitoringMonitor relation not found for monitoringId " + 
            request.getMonitoringId() + " and monitor code " + request.getMonitorCode()
        ));

        System.out.println("Estado actual: " + mm.getEstadoSeleccion());
        
        // Verificar que no esté ya aprobado/rechazado
        if ("aprobado".equalsIgnoreCase(mm.getEstadoSeleccion()) || 
            "rechazado".equalsIgnoreCase(mm.getEstadoSeleccion())) {
            throw new Exception("Esta postulación ya fue procesada anteriormente");
        }

        mm.setEstadoSeleccion("aprobado");
        mm.setComentarioDecision(request.getComentario());
        mm.setFechaDecision(LocalDateTime.now());
        mm.setDecididoPor(request.getDepartmentHeadId());
        
        MonitoringMonitor saved = monitoringMonitorRepository.save(mm);
        monitoringMonitorRepository.flush(); // Forzar el guardado inmediato
        
        System.out.println("Estado después de guardar: " + saved.getEstadoSeleccion());
        System.out.println("Postulación aprobada y guardada en BD");
        System.out.println("=============================");
    }

    @Override
    public void rejectApplication(ApproveApplicationRequest request) throws Exception {
        MonitoringMonitor mm = monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(
            request.getMonitoringId(), 
            request.getMonitorCode()
        ).orElseThrow(() -> new EntityNotFoundException(
            "MonitoringMonitor relation not found for monitoringId " + 
            request.getMonitoringId() + " and monitor code " + request.getMonitorCode()
        ));

        // Verificar que no esté ya aprobado/rechazado
        if ("aprobado".equalsIgnoreCase(mm.getEstadoSeleccion()) || 
            "rechazado".equalsIgnoreCase(mm.getEstadoSeleccion())) {
            throw new Exception("Esta postulación ya fue procesada anteriormente");
        }

        mm.setEstadoSeleccion("rechazado");
        mm.setComentarioDecision(request.getComentario());
        mm.setFechaDecision(LocalDateTime.now());
        mm.setDecididoPor(request.getDepartmentHeadId());
        
        monitoringMonitorRepository.save(mm);
    }

    // --- GenericService implementation ---
    @Override
    public List<MonitoringMonitor> findAll() {
        return monitoringMonitorRepository.findAll();
    }

    @Override
    public Optional<MonitoringMonitor> findById(Long id) {
        return monitoringMonitorRepository.findById(id);
    }

    @Override
    public MonitoringMonitor save(MonitoringMonitor entity) throws Exception {
        return monitoringMonitorRepository.save(entity);
    }

    @Override
    public MonitoringMonitor update(MonitoringMonitor entity) throws Exception {
        return monitoringMonitorRepository.save(entity);
    }

    @Override
    public void delete(MonitoringMonitor entity) throws Exception {
        monitoringMonitorRepository.delete(entity);
    }

    @Override
    public void deleteById(Long id) throws Exception {
        monitoringMonitorRepository.deleteById(id);
    }

    @Override
    public void validate(MonitoringMonitor entity) throws Exception {
        // No-op validation for this service
    }

    @Override
    public Long count() {
        return monitoringMonitorRepository.count();
    }
}