package com.pdg.sigma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MonitorEvaluationAssignmentDTO {

    private Long monitoringId;
    private Long monitoringMonitorId;
    private Long evaluationId;

    private String monitoringName;
    private String courseName;
    private String programName;
    private String semester;

    private String monitorCode;
    private String monitorIdentifier;
    private String monitorFullName;
    private String monitorEmail;

    private boolean evaluated;
    private double totalScore;
    private String performanceLevel;
    private boolean penaltyFlag;
    private boolean visibleToMonitor;
    private boolean acknowledgedByMonitor;
    private LocalDateTime evaluatedAt;

    private Integer taskCompliance;
    private Integer timelyCommunication;
    private Integer planFulfillment;
    private Integer attitude;
    private String comments;
}
