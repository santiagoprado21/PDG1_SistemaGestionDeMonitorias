package com.pdg.sigma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SupervisorEvaluationRequest {

    private Long monitoringId;
    private String monitorIdentifier;
    private Integer guidanceClarity;
    private Integer roleExpectations;
    private Integer availabilityDisposition;
    private Integer supportTimeliness;
    private Integer feedbackConstructive;
    private Integer feedbackFairness;
    private Integer respectfulTreatment;
    private Integer trustEnvironment;
    private String strengthsComments;
    private String improvementComments;
}
