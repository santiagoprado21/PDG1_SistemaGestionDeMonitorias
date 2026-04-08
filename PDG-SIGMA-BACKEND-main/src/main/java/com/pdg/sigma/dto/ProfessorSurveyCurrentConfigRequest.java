package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProfessorSurveyCurrentConfigRequest {
    private String semester;
    private List<Long> questionIds;
}

