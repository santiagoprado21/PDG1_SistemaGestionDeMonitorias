package com.pdg.sigma.service;

import com.pdg.sigma.dto.SupervisorEvaluationRequest;
import com.pdg.sigma.dto.SupervisorEvaluationResponse;
import com.pdg.sigma.dto.SupervisorEvaluationStatusDTO;

import java.util.List;
import java.util.Optional;

public interface SupervisorEvaluationService {

    SupervisorEvaluationResponse createEvaluation(String monitorIdentifier, SupervisorEvaluationRequest request) throws Exception;

    List<SupervisorEvaluationStatusDTO> getAssignmentsForMonitor(String monitorIdentifier) throws Exception;

    List<SupervisorEvaluationResponse> getEvaluationsForCoordinator() throws Exception;

    List<SupervisorEvaluationResponse> getEvaluationsByProfessor(String professorId) throws Exception;

    List<SupervisorEvaluationResponse> getEvaluationsByMonitor(String monitorIdentifier) throws Exception;

    Optional<SupervisorEvaluationResponse> getEvaluation(Long evaluationId);
}
