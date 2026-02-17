package com.pdg.sigma.repository;

import com.pdg.sigma.domain.SupervisorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupervisorEvaluationRepository extends JpaRepository<SupervisorEvaluation, Long> {

    Optional<SupervisorEvaluation> findByMonitoringIdAndMonitorCode(Long monitoringId, String monitorCode);

    List<SupervisorEvaluation> findByProfessorIdOrderByCreatedAtDesc(String professorId);

    List<SupervisorEvaluation> findByMonitorCodeOrderByCreatedAtDesc(String monitorCode);

    List<SupervisorEvaluation> findAllByOrderByCreatedAtDesc();
}
