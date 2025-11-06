package com.pdg.sigma.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pdg.sigma.domain.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * DTO para transferencia de datos de MonitoringRequest (Convocatoria de Monitoría)
 * Usado para crear nuevas convocatorias y mostrar información al frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringRequestDTO implements Serializable {
    
    private Long id;
    
    // IDs para crear/buscar
    private String professorId;
    private Long courseId;
    private Integer schoolId;
    private Long programId;
    
    // Nombres para mostrar en el frontend
    private String professorName;
    private String courseName;
    private String schoolName;
    private String programName;
    
    // Entidades completas (cuando se recupera de BD) - ignoradas en JSON
    @JsonIgnore
    private Professor professor;
    @JsonIgnore
    private Course course;
    @JsonIgnore
    private School school;
    @JsonIgnore
    private Program program;
    
    // Detalles de la solicitud (HU-010)
    private Integer requestedHours;
    private String justification;
    private String semester;
    private Date startDate;
    private Date finishDate;
    
    // Requisitos para postulantes
    private Double requiredAverageGrade;
    private Double requiredCourseGrade;
    private Double hourlyRate;
    
    // Estado y control
    private String status; // RequestStatus en String para el frontend
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Información adicional
    private Integer applicationCount; // Número de postulantes
    private String selectedMonitorName; // Nombre del monitor seleccionado (si existe)
    
    /**
     * Constructor para crear DTO desde entidad
     */
    public MonitoringRequestDTO(MonitoringRequest request) {
        this.id = request.getId();
        this.professor = request.getProfessor();
        this.professorId = request.getProfessor() != null ? request.getProfessor().getId() : null;
        this.professorName = request.getProfessor() != null ? request.getProfessor().getName() : null;
        
        this.course = request.getCourse();
        this.courseId = request.getCourse() != null ? request.getCourse().getId() : null;
        this.courseName = request.getCourse() != null ? request.getCourse().getName() : null;
        
        this.school = request.getSchool();
        this.schoolId = request.getSchool() != null ? request.getSchool().getId().intValue() : null;
        this.schoolName = request.getSchool() != null ? request.getSchool().getName() : null;
        
        this.program = request.getProgram();
        this.programId = request.getProgram() != null ? request.getProgram().getId() : null;
        this.programName = request.getProgram() != null ? request.getProgram().getName() : null;
        
        this.requestedHours = request.getRequestedHours();
        this.justification = request.getJustification();
        this.semester = request.getSemester();
        this.startDate = request.getStartDate();
        this.finishDate = request.getFinishDate();
        
        this.requiredAverageGrade = request.getRequiredAverageGrade();
        this.requiredCourseGrade = request.getRequiredCourseGrade();
        this.hourlyRate = request.getHourlyRate();
        
        this.status = request.getStatus() != null ? request.getStatus().name() : null;
        this.createdAt = request.getCreatedAt();
        this.updatedAt = request.getUpdatedAt();
        
        this.applicationCount = request.getApplicationCount();
    }
    
    /**
     * Constructor simplificado para crear nueva convocatoria
     */
    public MonitoringRequestDTO(String professorId, Long courseId, Integer schoolId, Long programId,
                                Integer requestedHours, String justification, String semester,
                                Date startDate, Date finishDate, Double requiredAverageGrade,
                                Double requiredCourseGrade, Double hourlyRate) {
        this.professorId = professorId;
        this.courseId = courseId;
        this.schoolId = schoolId;
        this.programId = programId;
        this.requestedHours = requestedHours;
        this.justification = justification;
        this.semester = semester;
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.requiredAverageGrade = requiredAverageGrade;
        this.requiredCourseGrade = requiredCourseGrade;
        this.hourlyRate = hourlyRate;
    }
    
    /**
     * Constructor para listado simple (solo información básica)
     */
    public MonitoringRequestDTO(Long id, String courseName, String professorName, 
                               Integer requestedHours, String semester, String status,
                               Integer applicationCount) {
        this.id = id;
        this.courseName = courseName;
        this.professorName = professorName;
        this.requestedHours = requestedHours;
        this.semester = semester;
        this.status = status;
        this.applicationCount = applicationCount;
    }
}

