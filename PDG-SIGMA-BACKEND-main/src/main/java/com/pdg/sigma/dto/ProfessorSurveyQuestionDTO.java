package com.pdg.sigma.dto;

import lombok.Data;

@Data
public class ProfessorSurveyQuestionDTO {
    private Long id;
    private String questionKey;
    private String statement;
    private String category;
    private boolean bankActive;
    private boolean selectedInCurrentSurvey;
    private Integer displayOrder;
}

