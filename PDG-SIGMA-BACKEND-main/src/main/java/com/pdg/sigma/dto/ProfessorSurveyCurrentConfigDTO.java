package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProfessorSurveyCurrentConfigDTO {
    private String semester;
    private List<ProfessorSurveyQuestionDTO> questions;
}

