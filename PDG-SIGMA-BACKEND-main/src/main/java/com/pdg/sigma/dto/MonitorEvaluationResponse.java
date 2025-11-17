package com.pdg.sigma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MonitorEvaluationResponse {

    private Long evaluationId;
    private Long monitoringId;
    private Long monitoringMonitorId;
    private String monitoringName;
    private String courseName;
    private String programName;
    private String semester;
    private String monitorCode;
    private String monitorIdentifier;
    private String monitorFullName;
    private String monitorEmail;
    private String professorId;

    private int taskCompliance;
    private int timelyCommunication;
    private int planFulfillment;
    private int attitude;

    private double totalScore;
    private String performanceLevel;
    private boolean penaltyFlag;
    private double penaltyWeight;

    private String comments;
    private boolean visibleToMonitor;
    private boolean acknowledgedByMonitor;
    private LocalDateTime acknowledgedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
