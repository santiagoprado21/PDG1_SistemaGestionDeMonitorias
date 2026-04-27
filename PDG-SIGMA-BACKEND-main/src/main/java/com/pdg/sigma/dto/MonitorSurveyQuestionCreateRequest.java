package com.pdg.sigma.dto;

import lombok.Data;

@Data
public class MonitorSurveyQuestionCreateRequest {
    private String statement;
    private String category;
}
