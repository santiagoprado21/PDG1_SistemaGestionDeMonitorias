package com.pdg.sigma.dto;

import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.MonitorApplication;
import com.pdg.sigma.domain.MonitoringRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de MonitorApplication (Postulación de Estudiante)
 * Usado para que estudiantes se postulen y profesores vean los postulantes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorApplicationDTO implements Serializable {
    
    private Long id;
    
    // IDs para crear/buscar
    private Long monitoringRequestId;
    private String monitorId; // idMonitor del estudiante
    private String monitorCode; // código del estudiante
    
    // Información del postulante
    private String monitorName;
    private String monitorLastName;
    private String monitorFullName;
    private String monitorEmail;
    private Integer monitorSemester;
    private Double gradeAverage;
    private Double gradeCourse;
    
    // Información de la convocatoria
    private String courseName;
    private String professorName;
    private Integer requestedHours;
    
    // Detalles de la postulación
    private String motivationLetter;
    private String status; // ApplicationStatus en String
    private LocalDateTime applicationDate;
    private LocalDateTime updatedAt;
    private String notes;
    
    /**
     * Constructor desde entidad completa
     */
    public MonitorApplicationDTO(MonitorApplication application) {
        this.id = application.getId();
        
        if (application.getMonitoringRequest() != null) {
            MonitoringRequest request = application.getMonitoringRequest();
            this.monitoringRequestId = request.getId();
            this.courseName = request.getCourse() != null ? request.getCourse().getName() : null;
            this.professorName = request.getProfessor() != null ? request.getProfessor().getName() : null;
            this.requestedHours = request.getRequestedHours();
        }
        
        if (application.getMonitor() != null) {
            Monitor monitor = application.getMonitor();
            this.monitorId = monitor.getIdMonitor();
            this.monitorCode = monitor.getCode();
            this.monitorName = monitor.getName();
            this.monitorLastName = monitor.getLastName();
            this.monitorFullName = monitor.getName() + " " + monitor.getLastName();
            this.monitorEmail = monitor.getEmail();
            this.monitorSemester = monitor.getSemester();
            this.gradeAverage = monitor.getGradeAverage();
            this.gradeCourse = monitor.getGradeCourse();
        }
        
        this.motivationLetter = application.getMotivationLetter();
        this.status = application.getStatus() != null ? application.getStatus().name() : null;
        this.applicationDate = application.getApplicationDate();
        this.updatedAt = application.getUpdatedAt();
        this.notes = application.getNotes();
    }
    
    /**
     * Constructor para crear nueva postulación desde el frontend
     */
    public MonitorApplicationDTO(Long monitoringRequestId, String monitorId, String motivationLetter) {
        this.monitoringRequestId = monitoringRequestId;
        this.monitorId = monitorId;
        this.motivationLetter = motivationLetter;
    }
    
    /**
     * Constructor simplificado para listar postulantes
     */
    public MonitorApplicationDTO(Long id, String monitorFullName, String monitorCode,
                                 Double gradeAverage, Double gradeCourse, Integer monitorSemester,
                                 String status, LocalDateTime applicationDate) {
        this.id = id;
        this.monitorFullName = monitorFullName;
        this.monitorCode = monitorCode;
        this.gradeAverage = gradeAverage;
        this.gradeCourse = gradeCourse;
        this.monitorSemester = monitorSemester;
        this.status = status;
        this.applicationDate = applicationDate;
    }
    
    /**
     * Constructor para vista del estudiante (sus postulaciones)
     */
    public MonitorApplicationDTO(Long id, String courseName, String professorName,
                                 Integer requestedHours, String status, 
                                 LocalDateTime applicationDate) {
        this.id = id;
        this.courseName = courseName;
        this.professorName = professorName;
        this.requestedHours = requestedHours;
        this.status = status;
        this.applicationDate = applicationDate;
    }
}

