package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveyIntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonitorSurveyIntegrationConfigRepository extends JpaRepository<MonitorSurveyIntegrationConfig, Long> {
    Optional<MonitorSurveyIntegrationConfig> findFirstByOrderByUpdatedAtDesc();
}
