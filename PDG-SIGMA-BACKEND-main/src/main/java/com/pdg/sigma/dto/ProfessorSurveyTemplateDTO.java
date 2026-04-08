package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProfessorSurveyTemplateDTO {
    private Long id;
    private String name;
    private String description;
    private String createdForSemester;
    private List<ProfessorSurveyQuestionDTO> questions;
}

