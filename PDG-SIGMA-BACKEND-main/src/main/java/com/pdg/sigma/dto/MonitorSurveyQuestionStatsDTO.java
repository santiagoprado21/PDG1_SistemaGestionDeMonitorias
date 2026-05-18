package com.pdg.sigma.dto;

import lombok.Data;

@Data
public class MonitorSurveyQuestionStatsDTO {
    private Long questionId;
    private String questionKey;
    private String statement;
    private String category;
    private double averageScore;
    private int responsesCount;
    private int minScore;
    private int maxScore;
}
