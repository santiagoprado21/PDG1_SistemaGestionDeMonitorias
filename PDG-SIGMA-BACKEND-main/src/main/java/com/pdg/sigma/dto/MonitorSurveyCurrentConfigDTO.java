package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class MonitorSurveyCurrentConfigDTO {
    private String semester;
    private List<MonitorSurveyQuestionDTO> questions;
}
