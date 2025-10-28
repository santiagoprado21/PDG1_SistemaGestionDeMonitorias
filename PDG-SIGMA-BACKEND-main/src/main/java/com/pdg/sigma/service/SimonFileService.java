package com.pdg.sigma.service;

import com.pdg.sigma.domain.SimonFileGeneration;
import com.pdg.sigma.dto.SimonMonitoringRowDTO;
import com.pdg.sigma.dto.SimonMonitoringDTO;

import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Workbook;
import java.util.List;
import java.util.Optional;

public interface SimonFileService {

    List<SimonMonitoringRowDTO> getApprovedMonitoringsForSimon(Optional<String> semester);

    ByteArrayOutputStream buildSimonWorkbook(List<SimonMonitoringRowDTO> rows) throws Exception;

    List<SimonFileGeneration> getGenerationHistory(Optional<String> semester);

    SimonFileGeneration auditGeneration(String generatedBy, String semester, int total, String fileName);

    // Legacy/tested API contracts (HU4 tests)
    Workbook generateSimonFile(String generatedBy, String semester) throws java.io.IOException;

    List<SimonMonitoringDTO> getApprovedMonitoringsForSimon();

    List<SimonFileGeneration> getGenerationHistory();

    List<SimonFileGeneration> getGenerationHistoryBySemester(String semester);
}

