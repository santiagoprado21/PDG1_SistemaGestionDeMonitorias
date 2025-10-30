package com.pdg.sigma.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.pdg.sigma.dto.MonitoringDTO;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "monitoring")
public class Monitoring implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "start_date", nullable = false)
    private Date start;

    @Column(name = "finish_date", nullable = false)
    private Date finish;

    @Column(name = "average_grade", nullable = true)
    private double averageGrade;

    @Column(name = "course_grade", nullable = true)
    private double courseGrade;

    @Column(name = "semester", nullable = false)
    private String semester;

    @ManyToOne
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    // Presupuesto por monitoría
    @Column(name = "estimated_hours")
    private Integer estimatedHours; // Horas planificadas para la monitoría

    @Column(name = "hourly_rate")
    private Double hourlyRate; // Valor de la hora (puede venir de política institucional)

    @OneToMany(mappedBy = "monitoring", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference //padre
    private List<MonitoringMonitor> monitoringMonitors;

    // ==================== NUEVO FLUJO HU-010: INTEGRACIÓN CON CONVOCATORIA ====================
    
    /**
     * Relación con la convocatoria (MonitoringRequest) que originó esta monitoría.
     * Null para monitorías creadas con el flujo antiguo (antes de HU-010).
     */
    @OneToOne
    @JoinColumn(name = "monitoring_request_id")
    private MonitoringRequest originatingRequest;

    /**
     * Monitor asignado desde la creación de la monitoría.
     * En el nuevo flujo, la monitoría se crea YA con el monitor seleccionado.
     */
    @ManyToOne
    @JoinColumn(name = "assigned_monitor_id")
    private Monitor assignedMonitor;

    /**
     * Estado de aprobación de la monitoría por parte del jefe de departamento.
     * En el nuevo flujo, el jefe aprueba el "paquete completo" (monitoría + monitor).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    private MonitoringApprovalStatus approvalStatus;

    /**
     * Justificación de la necesidad de esta monitoría (copiada de la MonitoringRequest).
     * Útil para que el jefe de departamento la vea al aprobar.
     */
    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    // ==================== AUDITORÍA DE APROBACIÓN ====================
    
    /**
     * ID del jefe de departamento que aprobó/rechazó la monitoría
     */
    @Column(name = "approved_by", length = 20)
    private String approvedBy;

    /**
     * Comentario del jefe de departamento sobre su decisión
     */
    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;

    /**
     * Fecha y hora de la aprobación/rechazo
     */
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    // ==================== CONSTRUCTORES ====================

    public Monitoring(School school, Program program, Course course,
                      Date start, Date finish, double averageGrade, double courseGrade, String semester, Professor professor) {
        this.school = school;
        this.program = program;
        this.course = course;
        this.start = start;
        this.finish = finish;
        this.averageGrade = averageGrade;
        this.courseGrade = courseGrade;
        this.semester = semester;
        this.professor =  professor;
    }

    public Monitoring() {

    }

    public Monitoring(MonitoringDTO monitoringDTO){
        this.school = monitoringDTO.getSchool();
        this.program = monitoringDTO.getProgram();
        this.course = monitoringDTO.getCourse();
        this.start = monitoringDTO.getStart();
        this.finish = monitoringDTO.getFinish();
        this.averageGrade = monitoringDTO.getAverageGrade();
        this.courseGrade = monitoringDTO.getCourseGrade();
        this.semester = monitoringDTO.getSemester();
        this.professor = monitoringDTO.getProfessor();
        this.estimatedHours = monitoringDTO.getEstimatedHours();
        this.hourlyRate = monitoringDTO.getHourlyRate();
    }

    /**
     * Constructor para crear una Monitoring desde una MonitoringRequest aprobada
     * (Nuevo flujo HU-010)
     */
    public Monitoring(MonitoringRequest request, Monitor assignedMonitor) {
        this.school = request.getSchool();
        this.program = request.getProgram();
        this.course = request.getCourse();
        this.start = request.getStartDate();
        this.finish = request.getFinishDate();
        this.averageGrade = request.getRequiredAverageGrade() != null ? request.getRequiredAverageGrade() : 4.0;
        this.courseGrade = request.getRequiredCourseGrade() != null ? request.getRequiredCourseGrade() : 4.0;
        this.semester = request.getSemester();
        this.professor = request.getProfessor();
        this.estimatedHours = request.getRequestedHours();
        this.hourlyRate = request.getHourlyRate();
        
        // Nuevos campos para HU-010
        this.originatingRequest = request;
        this.assignedMonitor = assignedMonitor;
        this.approvalStatus = MonitoringApprovalStatus.PENDIENTE_APROBACION;
        this.justification = request.getJustification();
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Verifica si esta monitoría requiere aprobación del jefe de departamento
     */
    public boolean requiresApproval() {
        return this.approvalStatus == MonitoringApprovalStatus.PENDIENTE_APROBACION;
    }

    /**
     * Verifica si la monitoría fue aprobada y puede funcionar
     */
    public boolean isApproved() {
        return this.approvalStatus == MonitoringApprovalStatus.APROBADA;
    }

    /**
     * Verifica si la monitoría fue rechazada
     */
    public boolean isRejected() {
        return this.approvalStatus == MonitoringApprovalStatus.RECHAZADA;
    }

    /**
     * Verifica si esta monitoría fue creada con el nuevo flujo (tiene request origen)
     */
    public boolean isFromNewFlow() {
        return this.originatingRequest != null;
    }

    /**
     * Aprueba la monitoría
     */
    public void approve(String approvedBy, String comment) {
        this.approvalStatus = MonitoringApprovalStatus.APROBADA;
        this.approvedBy = approvedBy;
        this.approvalComment = comment;
        this.approvalDate = LocalDateTime.now();
    }

    /**
     * Rechaza la monitoría
     */
    public void reject(String rejectedBy, String comment) {
        this.approvalStatus = MonitoringApprovalStatus.RECHAZADA;
        this.approvedBy = rejectedBy;
        this.approvalComment = comment;
        this.approvalDate = LocalDateTime.now();
    }
}