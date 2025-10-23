package com.pdg.sigma.service;

import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.dto.MonitorDTO;
import com.pdg.sigma.dto.ApproveApplicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pdg.sigma.repository.MonitoringMonitorRepository;

import jakarta.persistence.EntityNotFoundException;

import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.MonitoringMonitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public void deleteRelation(Long idMonitoring, String monitorCode) throws Exception {
        monitoringMonitorRepository.deleteByMonitoringIdAndMonitor_Code(idMonitoring, monitorCode.trim());
    }

    public void updateApplicantSelectionStatus(Long monitoringId, String monitorCode, String newStatus) {
    MonitoringMonitor mm = monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(monitoringId, monitorCode)
        .orElseThrow(() -> new EntityNotFoundException("MonitoringMonitor relation not found for monitoringId " + monitoringId + " and monitor code " + monitorCode));

    mm.setEstadoSeleccion(newStatus);
    monitoringMonitorRepository.save(mm);
}

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


}