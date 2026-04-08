package com.pdg.sigma.repository;

import com.pdg.sigma.domain.ProfessorSurveyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessorSurveyTemplateRepository extends JpaRepository<ProfessorSurveyTemplate, Long> {
    List<ProfessorSurveyTemplate> findAllByOrderByUpdatedAtDesc();
}

