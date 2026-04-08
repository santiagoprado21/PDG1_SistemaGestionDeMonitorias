package com.pdg.sigma.dto;

import lombok.Data;

@Data
public class MonitorSurveyPublicQuestionDTO {
    private Long id;
    private String questionKey;
    private String statement;
    private String category;
    private int displayOrder;
}
