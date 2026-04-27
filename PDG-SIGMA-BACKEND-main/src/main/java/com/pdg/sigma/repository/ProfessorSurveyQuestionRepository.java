package com.pdg.sigma.repository;

import com.pdg.sigma.domain.ProfessorSurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessorSurveyQuestionRepository extends JpaRepository<ProfessorSurveyQuestion, Long> {
    List<ProfessorSurveyQuestion> findAllByOrderByCreatedAtDesc();
    Optional<ProfessorSurveyQuestion> findByQuestionKey(String questionKey);
}

