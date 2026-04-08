package com.pdg.sigma.dto;

import lombok.Data;

import java.util.List;

@Data
public class MonitorSurveyCurrentConfigRequest {
    private String semester;
    private List<Long> questionIds;
}
