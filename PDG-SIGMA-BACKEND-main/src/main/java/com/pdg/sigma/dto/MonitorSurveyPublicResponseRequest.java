package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class MonitorSurveyPublicResponseRequest {
    private String semester;
    private String monitoringId;
    private String monitorCode;
    private String monitorName;
    private String positiveFeedback;
    private String improvementFeedback;
    private Double averageScore;
    private List<MonitorSurveyPublicResponseAnswerDTO> answers;
}
