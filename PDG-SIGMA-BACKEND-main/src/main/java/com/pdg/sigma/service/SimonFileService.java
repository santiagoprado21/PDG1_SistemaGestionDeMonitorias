package com.pdg.sigma.service;

import com.pdg.sigma.domain.SimonFileGeneration;
import com.pdg.sigma.dto.SimonMonitoringDTO;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.util.List;

public interface SimonFileService {
    
    /**
     * Genera el archivo Excel para SIMON con las monitorías aprobadas
     * @param generatedBy Usuario que genera el archivo
     * @param semester Semestre académico
     * @return Workbook de Apache POI con el archivo generado
     * @throws IOException Si hay error al generar el archivo
     */
    Workbook generateSimonFile(String generatedBy, String semester) throws IOException;
    
    /**
     * Obtiene las monitorías aprobadas en formato DTO para SIMON
     * @return Lista de DTOs con los datos formateados para SIMON
     */
    List<SimonMonitoringDTO> getApprovedMonitoringsForSimon();
    
    /**
     * Obtiene el historial de archivos generados
     * @return Lista de registros de generación
     */
    List<SimonFileGeneration> getGenerationHistory();
    
    /**
     * Obtiene el historial por semestre
     * @param semester Semestre académico
     * @return Lista de registros de generación del semestre
     */
    List<SimonFileGeneration> getGenerationHistoryBySemester(String semester);
}

