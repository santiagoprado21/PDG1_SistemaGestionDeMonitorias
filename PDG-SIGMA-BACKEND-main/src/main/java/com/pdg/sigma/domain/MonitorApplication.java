package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad que representa la postulación de un estudiante (Monitor) a una convocatoria de monitoría.
 * 
 * IMPORTANTE: Los estudiantes se postulan a MonitoringRequest (convocatoria),
 * NO a Monitoring (monitoría oficial que se crea después).
 * 
 * Cuando el profesor selecciona una de estas postulaciones, automáticamente:
 * - Esta MonitorApplication pasa a estado SELECCIONADO
 * - Las demás pasan a NO_SELECCIONADO
 * - Se crea una Monitoring oficial con el monitor asignado
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "monitor_application",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"monitoring_request_id", "monitor_id"},
           name = "uk_monitor_request"
       ))
public class MonitorApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // ==================== RELACIONES ====================
    
    /**
     * Convocatoria a la que el estudiante se postula
     * IMPORTANTE: Es MonitoringRequest, NO Monitoring
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_request_id", nullable = false)
    private MonitoringRequest monitoringRequest;

    /**
     * Estudiante (Monitor) que se postula
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    // ==================== INFORMACIÓN DE LA POSTULACIÓN ====================
    
    /**
     * Estado de la postulación
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.POSTULADO;

    /**
     * Fecha y hora en que el estudiante se postuló
     */
    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;

    /**
     * Carta de motivación opcional del estudiante
     */
    @Column(name = "motivation_letter", columnDefinition = "TEXT")
    private String motivationLetter;

    /**
     * Fecha y hora de última actualización de estado
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Comentarios adicionales del sistema o del profesor sobre la postulación
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ==================== CONSTRUCTORES ====================

    /**
     * Constructor para crear una nueva postulación
     */
    public MonitorApplication(MonitoringRequest monitoringRequest, Monitor monitor) {
        this.monitoringRequest = monitoringRequest;
        this.monitor = monitor;
        this.status = ApplicationStatus.POSTULADO;
        this.applicationDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor con carta de motivación
     */
    public MonitorApplication(MonitoringRequest monitoringRequest, Monitor monitor, String motivationLetter) {
        this(monitoringRequest, monitor);
        this.motivationLetter = motivationLetter;
    }

    // ==================== MÉTODOS DE CICLO DE VIDA JPA ====================

    /**
     * Establece el timestamp de creación antes de persistir
     */
    @PrePersist
    protected void onCreate() {
        if (this.applicationDate == null) {
            this.applicationDate = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Actualiza el timestamp de modificación
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Verifica si esta postulación fue seleccionada por el profesor
     */
    public boolean isSelected() {
        return this.status == ApplicationStatus.SELECCIONADO;
    }

    /**
     * Verifica si esta postulación está pendiente de revisión
     */
    public boolean isPending() {
        return this.status == ApplicationStatus.POSTULADO;
    }

    /**
     * Verifica si esta postulación fue descartada
     */
    public boolean isNotSelected() {
        return this.status == ApplicationStatus.NO_SELECCIONADO;
    }

    /**
     * Marca esta postulación como seleccionada
     */
    public void markAsSelected() {
        this.status = ApplicationStatus.SELECCIONADO;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca esta postulación como no seleccionada
     */
    public void markAsNotSelected() {
        this.status = ApplicationStatus.NO_SELECCIONADO;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Obtiene el nombre completo del postulante
     */
    public String getApplicantFullName() {
        if (this.monitor != null) {
            return this.monitor.getName() + " " + this.monitor.getLastName();
        }
        return "N/A";
    }

    /**
     * Obtiene el código del estudiante postulante
     */
    public String getApplicantCode() {
        return this.monitor != null ? this.monitor.getCode() : "N/A";
    }
}

