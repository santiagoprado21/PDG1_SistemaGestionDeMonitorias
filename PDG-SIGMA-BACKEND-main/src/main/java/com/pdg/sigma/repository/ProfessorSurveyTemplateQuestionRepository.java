package com.pdg.sigma.repository;

import com.pdg.sigma.domain.ProfessorSurveyTemplateQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessorSurveyTemplateQuestionRepository extends JpaRepository<ProfessorSurveyTemplateQuestion, Long> {
    List<ProfessorSurveyTemplateQuestion> findByTemplateIdOrderByDisplayOrderAsc(Long templateId);
    void deleteByTemplateId(Long templateId);
}

