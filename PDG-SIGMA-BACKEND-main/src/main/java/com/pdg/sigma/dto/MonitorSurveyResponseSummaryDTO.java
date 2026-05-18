package com.pdg.sigma.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MonitorSurveyResponseSummaryDTO {
    private Long responseId;
    private String semester;
    private String monitoringId;
    private String monitorCode;
    private String monitorName;
    private Double averageScore;
    private String positiveFeedback;
    private String improvementFeedback;
    private LocalDateTime createdAt;
}
