package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorSurveyTemplateRepository extends JpaRepository<MonitorSurveyTemplate, Long> {
    List<MonitorSurveyTemplate> findAllByOrderByUpdatedAtDesc();
}
