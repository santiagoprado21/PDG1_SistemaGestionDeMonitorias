package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entidad que representa una Convocatoria/Postulación de Monitoría creada por un profesor.
 * Los estudiantes se postulan a esta convocatoria, y cuando el profesor selecciona uno,
 * se crea automáticamente la Monitoring oficial.
 * 
 * Esta es la implementación de la HU-010: "Crear postulación de monitorias por parte de los profesores"
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "monitoring_request")
public class MonitoringRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // ==================== INFORMACIÓN DEL PROFESOR ====================
    
    /**
     * Profesor que crea la postulación/convocatoria de monitoría
     */
    @ManyToOne
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    // ==================== INFORMACIÓN ACADÉMICA ====================
    
    /**
     * Curso para el cual se solicita la monitoría
     */
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    // ==================== DETALLES DE LA SOLICITUD (HU-010) ====================
    
    /**
     * Horas solicitadas para la monitoría (HU-010: especificar horas solicitadas)
     */
    @Column(name = "requested_hours", nullable = false)
    private Integer requestedHours;

    /**
     * Justificación del profesor para solicitar la monitoría (HU-010: especificar justificación)
     */
    @Column(name = "justification", nullable = false, columnDefinition = "TEXT")
    private String justification;

    /**
     * Semestre académico (formato: YYYY-S, ejemplo: 2025-1)
     */
    @Column(name = "semester", nullable = false, length = 8)
    private String semester;

    /**
     * Fecha de inicio de la monitoría
     */
    @Column(name = "start_date", nullable = false)
    private Date startDate;

    /**
     * Fecha de finalización de la monitoría
     */
    @Column(name = "finish_date", nullable = false)
    private Date finishDate;

    // ==================== REQUISITOS PARA POSTULANTES ====================
    
    /**
     * Promedio acumulado mínimo requerido para postularse
     */
    @Column(name = "required_average_grade")
    private Double requiredAverageGrade;

    /**
     * Nota mínima en el curso requerida para postularse
     */
    @Column(name = "required_course_grade")
    private Double requiredCourseGrade;

    /**
     * Valor por hora a pagar al monitor (puede venir de configuración institucional)
     */
    @Column(name = "hourly_rate")
    private Double hourlyRate;

    // ==================== ESTADO Y CONTROL ====================
    
    /**
     * Estado actual de la convocatoria/postulación
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RequestStatus status = RequestStatus.CONVOCATORIA_ABIERTA;

    /**
     * Fecha y hora de creación de la convocatoria
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de última actualización
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== RELACIONES ====================
    
    /**
     * Lista de postulaciones de estudiantes a esta convocatoria
     */
    @OneToMany(mappedBy = "monitoringRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MonitorApplication> studentApplications = new ArrayList<>();

    /**
     * Monitoría oficial creada cuando se selecciona un monitor
     * (será null mientras está en estado CONVOCATORIA_ABIERTA)
     */
    @OneToOne(mappedBy = "originatingRequest", fetch = FetchType.LAZY)
    private Monitoring createdMonitoring;

    // ==================== CONSTRUCTORES Y MÉTODOS DE UTILIDAD ====================

    /**
     * Constructor para crear una nueva convocatoria
     */
    public MonitoringRequest(Professor professor, Course course, School school, Program program,
                           Integer requestedHours, String justification, String semester,
                           Date startDate, Date finishDate, Double requiredAverageGrade,
                           Double requiredCourseGrade, Double hourlyRate) {
        this.professor = professor;
        this.course = course;
        this.school = school;
        this.program = program;
        this.requestedHours = requestedHours;
        this.justification = justification;
        this.semester = semester;
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.requiredAverageGrade = requiredAverageGrade;
        this.requiredCourseGrade = requiredCourseGrade;
        this.hourlyRate = hourlyRate;
        this.status = RequestStatus.CONVOCATORIA_ABIERTA;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Actualiza el timestamp de modificación
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Establece el timestamp de creación antes de persistir
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Verifica si la convocatoria está abierta para postulaciones
     */
    public boolean isOpenForApplications() {
        return this.status == RequestStatus.CONVOCATORIA_ABIERTA;
    }

    /**
     * Verifica si ya se seleccionó un monitor
     */
    public boolean hasSelectedMonitor() {
        return this.status == RequestStatus.MONITOR_SELECCIONADO ||
               this.status == RequestStatus.PENDIENTE_APROBACION ||
               this.status == RequestStatus.APROBADA;
    }

    /**
     * Cuenta el número de postulaciones recibidas
     */
    public int getApplicationCount() {
        return this.studentApplications != null ? this.studentApplications.size() : 0;
    }
}

