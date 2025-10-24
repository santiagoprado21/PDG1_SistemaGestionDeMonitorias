package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveApplicationRequest {
    private Long monitoringId;
    private String monitorCode;
    private String comentario;
    private String departmentHeadId;
}

