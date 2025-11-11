package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonitorEvaluationRepository extends JpaRepository<MonitorEvaluation, Long> {

    Optional<MonitorEvaluation> findByMonitoringIdAndMonitorCode(Long monitoringId, String monitorCode);

    List<MonitorEvaluation> findByProfessorIdOrderByCreatedAtDesc(String professorId);

    List<MonitorEvaluation> findByMonitorCodeOrderByCreatedAtDesc(String monitorCode);

    Optional<MonitorEvaluation> findByMonitoringMonitorId(Long monitoringMonitorId);

    @Query("SELECT e FROM MonitorEvaluation e WHERE e.monitoring.id IN :monitoringIds")
    List<MonitorEvaluation> findByMonitoringIds(@Param("monitoringIds") Collection<Long> monitoringIds);
}
