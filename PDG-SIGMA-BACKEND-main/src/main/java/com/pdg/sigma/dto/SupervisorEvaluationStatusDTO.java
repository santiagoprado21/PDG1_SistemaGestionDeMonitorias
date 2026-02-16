package com.pdg.sigma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SupervisorEvaluationStatusDTO {

    private Long monitoringId;
    private Long monitoringMonitorId;
    private Long evaluationId;

    private String monitoringName;
    private String courseName;
    private String programName;
    private String semester;

    private String professorId;
    private String professorName;

    private boolean evaluated;
    private String status;
    private LocalDateTime submittedAt;
}
