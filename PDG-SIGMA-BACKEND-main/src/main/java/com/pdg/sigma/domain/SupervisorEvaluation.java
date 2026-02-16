package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "supervisor_evaluation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_supervisor_eval", columnNames = {"monitoring_id", "monitor_code"})
})
public class SupervisorEvaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final double PERFORMANCE_THRESHOLD = 4.0;

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

    @Column(name = "guidance_clarity", nullable = false)
    private int guidanceClarity;

    @Column(name = "role_expectations", nullable = false)
    private int roleExpectations;

    @Column(name = "availability_disposition", nullable = false)
    private int availabilityDisposition;

    @Column(name = "support_timeliness", nullable = false)
    private int supportTimeliness;

    @Column(name = "feedback_constructive", nullable = false)
    private int feedbackConstructive;

    @Column(name = "feedback_fairness", nullable = false)
    private int feedbackFairness;

    @Column(name = "respectful_treatment", nullable = false)
    private int respectfulTreatment;

    @Column(name = "trust_environment", nullable = false)
    private int trustEnvironment;

    @Column(name = "total_score", nullable = false)
    private double totalScore;

    @Column(name = "performance_level", length = 30, nullable = false)
    private String performanceLevel;

    @Column(name = "strengths_comments", columnDefinition = "TEXT")
    private String strengthsComments;

    @Column(name = "improvement_comments", columnDefinition = "TEXT")
    private String improvementComments;

    @Column(name = "submitted_by", length = 50)
    private String submittedBy;

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

    public void applyScores(
            int guidanceClarity,
            int roleExpectations,
            int availabilityDisposition,
            int supportTimeliness,
            int feedbackConstructive,
            int feedbackFairness,
            int respectfulTreatment,
            int trustEnvironment,
            String strengthsComments,
            String improvementComments
    ) {
        this.guidanceClarity = guidanceClarity;
        this.roleExpectations = roleExpectations;
        this.availabilityDisposition = availabilityDisposition;
        this.supportTimeliness = supportTimeliness;
        this.feedbackConstructive = feedbackConstructive;
        this.feedbackFairness = feedbackFairness;
        this.respectfulTreatment = respectfulTreatment;
        this.trustEnvironment = trustEnvironment;
        this.strengthsComments = strengthsComments;
        this.improvementComments = improvementComments;
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.totalScore = Math.round(((guidanceClarity + roleExpectations + availabilityDisposition + supportTimeliness
            + feedbackConstructive + feedbackFairness + respectfulTreatment + trustEnvironment) / 8.0) * 100.0) / 100.0;
        this.performanceLevel = resolvePerformanceLevel(this.totalScore);
    }

    private String resolvePerformanceLevel(double score) {
        if (score >= 6.0) {
            return "EXCELENTE";
        }
        if (score >= 5.0) {
            return "DESTACADO";
        }
        if (score >= PERFORMANCE_THRESHOLD) {
            return "ADECUADO";
        }
        return "EN_RIESGO";
    }
}
