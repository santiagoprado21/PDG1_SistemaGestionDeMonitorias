package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingApplicationDTO {
    private Long id;
    private Long monitoringId;
    private String courseName;
    private String professorName;
    private String monitorName;
    private String monitorCode;
    private String monitorEmail;
    private Double gradeAverage;
    private Double gradeCourse;
    private Integer semester;
    private String estadoSeleccion;
    private String comentarioDecision;
    private LocalDateTime fechaDecision;
    private String decididoPor;
    private String programName;
    private String schoolName;
}

