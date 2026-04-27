package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "monitor_survey_response")
public class MonitorSurveyResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "semester", nullable = false, length = 20)
    private String semester;

    @Column(name = "monitoring_id", length = 50)
    private String monitoringId;

    @Column(name = "monitor_code", length = 50)
    private String monitorCode;

    @Column(name = "monitor_name", length = 255)
    private String monitorName;

    @Column(name = "positive_feedback", columnDefinition = "TEXT")
    private String positiveFeedback;

    @Column(name = "improvement_feedback", columnDefinition = "TEXT")
    private String improvementFeedback;

    @Column(name = "average_score")
    private Double averageScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
