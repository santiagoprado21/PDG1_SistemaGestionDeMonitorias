package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProfessorSurveyTemplateUpdateRequest {
    private String name;
    private String description;
    private String createdForSemester;
    private List<Long> questionIds;
}

