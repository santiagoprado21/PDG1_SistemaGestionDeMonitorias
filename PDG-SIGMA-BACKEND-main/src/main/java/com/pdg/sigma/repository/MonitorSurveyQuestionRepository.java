package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitorSurveyQuestionRepository extends JpaRepository<MonitorSurveyQuestion, Long> {
    List<MonitorSurveyQuestion> findAllByOrderByCreatedAtDesc();
    Optional<MonitorSurveyQuestion> findByQuestionKey(String questionKey);
}
