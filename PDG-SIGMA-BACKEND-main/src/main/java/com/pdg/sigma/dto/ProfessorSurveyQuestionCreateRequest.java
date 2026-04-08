package com.pdg.sigma.dto;

import lombok.Data;

@Data
public class ProfessorSurveyQuestionCreateRequest {
    private String statement;
    private String category;
}

