package com.pdg.sigma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MonitorEvaluationRequest {

    private String professorId;
    private Long monitoringId;
    private String monitorCode;
    private Integer taskCompliance;
    private Integer timelyCommunication;
    private Integer planFulfillment;
    private Integer attitude;
    private String comments;
    private Boolean visibleToMonitor;
}
