package com.pdg.sigma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SupervisorEvaluationResponse {

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
    private String professorName;

    private int guidanceClarity;
    private int roleExpectations;
    private int availabilityDisposition;
    private int supportTimeliness;
    private int feedbackConstructive;
    private int feedbackFairness;
    private int respectfulTreatment;
    private int trustEnvironment;

    private double totalScore;
    private String performanceLevel;

    private String strengthsComments;
    private String improvementComments;
    private String submittedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
