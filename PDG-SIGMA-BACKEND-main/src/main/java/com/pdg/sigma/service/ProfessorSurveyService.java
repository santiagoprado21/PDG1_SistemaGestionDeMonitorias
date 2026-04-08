package com.pdg.sigma.service;

import com.pdg.sigma.dto.*;

import java.util.List;

public interface ProfessorSurveyService {
    List<ProfessorSurveyQuestionDTO> getQuestionBank(String semester);
    ProfessorSurveyQuestionDTO createQuestion(ProfessorSurveyQuestionCreateRequest request) throws Exception;
    ProfessorSurveyQuestionDTO updateQuestion(Long questionId, ProfessorSurveyQuestionUpdateRequest request, String semester) throws Exception;
    ProfessorSurveyQuestionDTO updateQuestionStatus(Long questionId, boolean bankActive) throws Exception;

    ProfessorSurveyCurrentConfigDTO getCurrentConfig(String semester);
    ProfessorSurveyCurrentConfigDTO saveCurrentConfig(ProfessorSurveyCurrentConfigRequest request) throws Exception;

    List<ProfessorSurveyTemplateDTO> listTemplates();
    ProfessorSurveyTemplateDTO createTemplate(ProfessorSurveyTemplateCreateRequest request) throws Exception;
    ProfessorSurveyTemplateDTO updateTemplate(Long templateId, ProfessorSurveyTemplateUpdateRequest request) throws Exception;
    void deleteTemplate(Long templateId) throws Exception;
    ProfessorSurveyCurrentConfigDTO applyTemplate(ProfessorSurveyApplyTemplateRequest request) throws Exception;
}

