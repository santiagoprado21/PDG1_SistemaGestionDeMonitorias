package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimonPreviewResponse {
    private int totalMonitorings;
    private boolean canGenerate;
    private List<SimonMonitoringRowDTO> monitorings;
}
