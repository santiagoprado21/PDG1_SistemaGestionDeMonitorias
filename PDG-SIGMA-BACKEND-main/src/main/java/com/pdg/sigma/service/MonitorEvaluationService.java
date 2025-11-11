package com.pdg.sigma.service;

import com.pdg.sigma.dto.MonitorEvaluationAssignmentDTO;
import com.pdg.sigma.dto.MonitorEvaluationRequest;
import com.pdg.sigma.dto.MonitorEvaluationResponse;

import java.util.List;
import java.util.Optional;

public interface MonitorEvaluationService {

    MonitorEvaluationResponse createEvaluation(String professorId, MonitorEvaluationRequest request) throws Exception;

    MonitorEvaluationResponse updateEvaluation(Long evaluationId, String professorId, MonitorEvaluationRequest request) throws Exception;

    List<MonitorEvaluationResponse> getEvaluationsByProfessor(String professorId) throws Exception;

    List<MonitorEvaluationAssignmentDTO> getEvaluationAssignmentsForProfessor(String professorId, Optional<String> search) throws Exception;

    List<MonitorEvaluationResponse> getEvaluationsForMonitor(String monitorIdentifier) throws Exception;

    MonitorEvaluationResponse acknowledgeEvaluation(Long evaluationId, String monitorIdentifier) throws Exception;

    Optional<MonitorEvaluationResponse> getEvaluation(Long evaluationId);
}
