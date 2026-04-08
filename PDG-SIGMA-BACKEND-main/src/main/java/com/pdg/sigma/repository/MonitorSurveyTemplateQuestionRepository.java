package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveyTemplateQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorSurveyTemplateQuestionRepository extends JpaRepository<MonitorSurveyTemplateQuestion, Long> {
    List<MonitorSurveyTemplateQuestion> findByTemplateIdOrderByDisplayOrderAsc(Long templateId);
    void deleteByTemplateId(Long templateId);
}
