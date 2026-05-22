package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class MonitorSurveyReportDTO {
    private String semester;
    private int totalResponses;
    private double averageScore;
    private int totalAnswers;
    private List<MonitorSurveyQuestionStatsDTO> questionStats;
    private List<MonitorSurveyResponseSummaryDTO> responses;
}
