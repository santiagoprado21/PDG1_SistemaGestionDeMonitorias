package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.controller.MonitorSurveyController;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.service.MonitorSurveyService;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MonitorSurveyController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitorSurveyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MonitorSurveyService monitorSurveyService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getQuestionBank_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.getQuestionBank(any())).thenReturn(List.of(new MonitorSurveyQuestionDTO()));

        mockMvc.perform(get("/monitor-survey/admin/questions")
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getQuestionBank_nonDepartmentHead_returnsForbidden() throws Exception {
        mockMvc.perform(get("/monitor-survey/admin/questions")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void createQuestion_asDepartmentHead_returnsCreated() throws Exception {
        MonitorSurveyQuestionDTO result = new MonitorSurveyQuestionDTO();
        result.setId(1L);
        when(monitorSurveyService.createQuestion(any())).thenReturn(result);

        mockMvc.perform(post("/monitor-survey/admin/questions")
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statement\":\"Test\",\"category\":\"Cat\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createQuestion_throws_returnsBadRequest() throws Exception {
        when(monitorSurveyService.createQuestion(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitor-survey/admin/questions")
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statement\":\"Test\",\"category\":\"Cat\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateQuestion_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.updateQuestion(any(), any(), any())).thenReturn(new MonitorSurveyQuestionDTO());

        mockMvc.perform(put("/monitor-survey/admin/questions/{questionId}", 1L)
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statement\":\"Updated\",\"category\":\"Cat\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateQuestionStatus_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.updateQuestionStatus(anyLong(), anyBoolean())).thenReturn(new MonitorSurveyQuestionDTO());

        mockMvc.perform(patch("/monitor-survey/admin/questions/{questionId}/status", 1L)
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankActive\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateQuestionStatus_nullBankActive_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/monitor-survey/admin/questions/{questionId}/status", 1L)
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe indicar el estado de la pregunta"));
    }

    @Test
    void getCurrentConfig_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.getCurrentConfig(any())).thenReturn(new MonitorSurveyCurrentConfigDTO());

        mockMvc.perform(get("/monitor-survey/admin/current-config")
                        .requestAttr("role", "jfedpto"))
                .andExpect(status().isOk());
    }

    @Test
    void validatePeriod_asDepartmentHead_returnsOk() throws Exception {
        mockMvc.perform(get("/monitor-survey/admin/validate-period")
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validatePeriod_invalidYear_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/monitor-survey/admin/validate-period")
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2020-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveCurrentConfig_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.saveCurrentConfig(any())).thenReturn(new MonitorSurveyCurrentConfigDTO());

        mockMvc.perform(put("/monitor-survey/admin/current-config")
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semester\":\"2026-1\",\"questionIds\":[1,2]}"))
                .andExpect(status().isOk());
    }

    @Test
    void listTemplates_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.listTemplates()).thenReturn(List.of(new MonitorSurveyTemplateDTO()));

        mockMvc.perform(get("/monitor-survey/admin/templates")
                        .requestAttr("role", "jfedpto"))
                .andExpect(status().isOk());
    }

    @Test
    void createTemplate_asDepartmentHead_returnsCreated() throws Exception {
        when(monitorSurveyService.createTemplate(any())).thenReturn(new MonitorSurveyTemplateDTO());

        mockMvc.perform(post("/monitor-survey/admin/templates")
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"T1\",\"createdForSemester\":\"2026-1\",\"questionIds\":[1]}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateTemplate_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.updateTemplate(any(), any())).thenReturn(new MonitorSurveyTemplateDTO());

        mockMvc.perform(put("/monitor-survey/admin/templates/{templateId}", 1L)
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"T1\",\"createdForSemester\":\"2026-1\",\"questionIds\":[1]}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTemplate_asDepartmentHead_returnsOk() throws Exception {
        doNothing().when(monitorSurveyService).deleteTemplate(1L);

        mockMvc.perform(delete("/monitor-survey/admin/templates/{templateId}", 1L)
                        .requestAttr("role", "jfedpto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Plantilla eliminada"));
    }

    @Test
    void applyTemplate_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.applyTemplate(any())).thenReturn(new MonitorSurveyCurrentConfigDTO());

        mockMvc.perform(post("/monitor-survey/admin/apply-template")
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semester\":\"2026-1\",\"templateId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicQuestions_returnsOk() throws Exception {
        when(monitorSurveyService.getPublicQuestions(any())).thenReturn(List.of(new MonitorSurveyPublicQuestionDTO()));

        mockMvc.perform(get("/monitor-survey/public/questions")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicCurrentConfig_returnsOk() throws Exception {
        when(monitorSurveyService.getCurrentConfig(any())).thenReturn(new MonitorSurveyCurrentConfigDTO());

        mockMvc.perform(get("/monitor-survey/public/current-config")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk());
    }

    @Test
    void storePublicResponse_returnsCreated() throws Exception {
        doNothing().when(monitorSurveyService).storePublicResponse(any());

        mockMvc.perform(post("/monitor-survey/public/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semester\":\"2026-1\",\"answers\":[{\"questionId\":1,\"score\":5}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Respuesta registrada"));
    }

    @Test
    void getIntegrationConfig_returnsOk() throws Exception {
        when(monitorSurveyService.getIntegrationConfig()).thenReturn(new MonitorSurveyIntegrationConfigDTO());

        mockMvc.perform(get("/monitor-survey/public/integration-config"))
                .andExpect(status().isOk());
    }

    @Test
    void saveIntegrationConfig_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.saveIntegrationConfig(any())).thenReturn(new MonitorSurveyIntegrationConfigDTO());

        mockMvc.perform(put("/monitor-survey/admin/integration-config")
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void getSurveyReport_asDepartmentHead_returnsOk() throws Exception {
        when(monitorSurveyService.getSurveyReport(any(), any(), any())).thenReturn(new MonitorSurveyReportDTO());

        mockMvc.perform(get("/monitor-survey/admin/report")
                        .requestAttr("role", "jfedpto"))
                .andExpect(status().isOk());
    }

    @Test
    void exportSurveyReportCsv_asDepartmentHead_returnsCsv() throws Exception {
        when(monitorSurveyService.exportSurveyReportCsv(any(), any(), any())).thenReturn("csv,data");

        mockMvc.perform(get("/monitor-survey/admin/report/csv")
                        .requestAttr("role", "jfedpto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    void endpoints_nonDepartmentHead_returnsForbidden() throws Exception {
        mockMvc.perform(get("/monitor-survey/admin/questions")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/monitor-survey/admin/current-config")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/monitor-survey/admin/templates")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/monitor-survey/admin/report")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden());
    }
}
