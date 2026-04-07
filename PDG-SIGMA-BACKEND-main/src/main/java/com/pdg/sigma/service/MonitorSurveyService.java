package com.pdg.sigma.service;

import com.pdg.sigma.dto.*;

import java.util.List;

public interface MonitorSurveyService {
    List<MonitorSurveyQuestionDTO> getQuestionBank(String semester);
    MonitorSurveyQuestionDTO createQuestion(MonitorSurveyQuestionCreateRequest request) throws Exception;
    MonitorSurveyQuestionDTO updateQuestion(Long questionId, MonitorSurveyQuestionUpdateRequest request, String semester) throws Exception;
    MonitorSurveyQuestionDTO updateQuestionStatus(Long questionId, boolean bankActive) throws Exception;

    MonitorSurveyCurrentConfigDTO getCurrentConfig(String semester);
    MonitorSurveyCurrentConfigDTO saveCurrentConfig(MonitorSurveyCurrentConfigRequest request) throws Exception;

    List<MonitorSurveyTemplateDTO> listTemplates();
    MonitorSurveyTemplateDTO createTemplate(MonitorSurveyTemplateCreateRequest request) throws Exception;
    MonitorSurveyTemplateDTO updateTemplate(Long templateId, MonitorSurveyTemplateUpdateRequest request) throws Exception;
    void deleteTemplate(Long templateId) throws Exception;
    MonitorSurveyCurrentConfigDTO applyTemplate(MonitorSurveyApplyTemplateRequest request) throws Exception;

    List<MonitorSurveyPublicQuestionDTO> getPublicQuestions(String semester);
    void storePublicResponse(MonitorSurveyPublicResponseRequest request) throws Exception;
}
