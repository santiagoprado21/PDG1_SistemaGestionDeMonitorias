package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveyResponseAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorSurveyResponseAnswerRepository extends JpaRepository<MonitorSurveyResponseAnswer, Long> {
    boolean existsByQuestionIdAndResponseSemester(Long questionId, String semester);
}
