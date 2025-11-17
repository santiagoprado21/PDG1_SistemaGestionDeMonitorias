package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "monitor_evaluation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_monitoring_monitor", columnNames = {"monitoring_id", "monitor_code"})
})
public class MonitorEvaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final double PENALTY_THRESHOLD = 3.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_id", nullable = false)
    private Monitoring monitoring;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_code", referencedColumnName = "code", nullable = false)
    private Monitor monitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_monitor_id")
    private MonitoringMonitor monitoringMonitor;

    @Column(name = "task_compliance", nullable = false)
    private int taskCompliance;

    @Column(name = "timely_communication", nullable = false)
    private int timelyCommunication;

    @Column(name = "plan_fulfillment", nullable = false)
    private int planFulfillment;

    @Column(name = "attitude", nullable = false)
    private int attitude;

    @Column(name = "total_score", nullable = false)
    private double totalScore;

    @Column(name = "performance_level", length = 30, nullable = false)
    private String performanceLevel;

    @Column(name = "penalty_flag", nullable = false)
    private boolean penaltyFlag;

    @Column(name = "penalty_weight", nullable = false)
    private double penaltyWeight;

    @Column(name = "qualitative_feedback", columnDefinition = "TEXT")
    private String qualitativeFeedback;

    @Column(name = "visible_to_monitor", nullable = false)
    private boolean visibleToMonitor = true;

    @Column(name = "acknowledged_by_monitor", nullable = false)
    private boolean acknowledgedByMonitor = false;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "semester", length = 20)
    private String semester;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void applyScores(int taskCompliance, int timelyCommunication, int planFulfillment, int attitude, String feedback) {
        this.taskCompliance = taskCompliance;
        this.timelyCommunication = timelyCommunication;
        this.planFulfillment = planFulfillment;
        this.attitude = attitude;
        this.qualitativeFeedback = feedback;
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.totalScore = Math.round(((taskCompliance + timelyCommunication + planFulfillment + attitude) / 4.0) * 100.0) / 100.0;
        this.performanceLevel = resolvePerformanceLevel(this.totalScore);
        this.penaltyFlag = this.totalScore < PENALTY_THRESHOLD;
        this.penaltyWeight = Math.max(0.0, Math.round((PENALTY_THRESHOLD - this.totalScore) * 100.0) / 100.0);
    }

    private String resolvePerformanceLevel(double score) {
        if (score >= 4.5) {
            return "EXCELENTE";
        }
        if (score >= 3.5) {
            return "DESTACADO";
        }
        if (score >= 3.0) {
            return "ADECUADO";
        }
        return "EN_RIESGO";
    }

    public void markAcknowledged() {
        this.acknowledgedByMonitor = true;
        this.acknowledgedAt = LocalDateTime.now();
    }
}
