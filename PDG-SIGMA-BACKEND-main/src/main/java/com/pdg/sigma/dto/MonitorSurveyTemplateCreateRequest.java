package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class MonitorSurveyTemplateCreateRequest {
    private String name;
    private String description;
    private List<Long> questionIds;
}
