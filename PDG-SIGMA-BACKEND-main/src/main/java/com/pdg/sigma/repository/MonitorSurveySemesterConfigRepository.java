package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveySemesterConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitorSurveySemesterConfigRepository extends JpaRepository<MonitorSurveySemesterConfig, Long> {
    Optional<MonitorSurveySemesterConfig> findBySemester(String semester);
    Optional<MonitorSurveySemesterConfig> findFirstByActiveTrueOrderByUpdatedAtDesc();
    List<MonitorSurveySemesterConfig> findAllByOrderByUpdatedAtDesc();
    List<MonitorSurveySemesterConfig> findAllByTemplateId(Long templateId);
}
