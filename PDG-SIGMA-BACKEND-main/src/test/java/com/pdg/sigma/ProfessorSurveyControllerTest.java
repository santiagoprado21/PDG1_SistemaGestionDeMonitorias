package com.pdg.sigma;

import com.pdg.sigma.controller.ProfessorSurveyController;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.service.ProfessorSurveyService;
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

@WebMvcTest(controllers = ProfessorSurveyController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class ProfessorSurveyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfessorSurveyService professorSurveyService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String ROLE_HEAD = "jfedpto";

    // ---- GET /professor-survey/admin/questions ----

    @Test
    void getQuestionBank_asHead_success() throws Exception {
        when(professorSurveyService.getQuestionBank(any())).thenReturn(List.of(new ProfessorSurveyQuestionDTO()));

        mockMvc.perform(get("/professor-survey/admin/questions")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getQuestionBank_asHead_withSemester() throws Exception {
        when(professorSurveyService.getQuestionBank(eq("2026-1"))).thenReturn(List.of());

        mockMvc.perform(get("/professor-survey/admin/questions")
                        .param("semester", "2026-1")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());
    }

    @Test
    void getQuestionBank_notHead_returns403() throws Exception {
        mockMvc.perform(get("/professor-survey/admin/questions")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void getQuestionBank_error_returns400() throws Exception {
        when(professorSurveyService.getQuestionBank(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/professor-survey/admin/questions")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- POST /professor-survey/admin/questions ----

    @Test
    void createQuestion_asHead_success() throws Exception {
        when(professorSurveyService.createQuestion(any())).thenReturn(new ProfessorSurveyQuestionDTO());

        mockMvc.perform(post("/professor-survey/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"¿Pregunta?\"}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isCreated());
    }

    @Test
    void createQuestion_notHead_returns403() throws Exception {
        mockMvc.perform(post("/professor-survey/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createQuestion_error_returns400() throws Exception {
        when(professorSurveyService.createQuestion(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/professor-survey/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /professor-survey/admin/questions/{questionId} ----

    @Test
    void updateQuestion_asHead_success() throws Exception {
        when(professorSurveyService.updateQuestion(anyLong(), any(), any())).thenReturn(new ProfessorSurveyQuestionDTO());

        mockMvc.perform(put("/professor-survey/admin/questions/{questionId}", 1L)
                        .param("semester", "2026-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"Actualizada\"}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());
    }

    @Test
    void updateQuestion_notHead_returns403() throws Exception {
        mockMvc.perform(put("/professor-survey/admin/questions/{questionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuestion_error_returns400() throws Exception {
        when(professorSurveyService.updateQuestion(anyLong(), any(), any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(put("/professor-survey/admin/questions/{questionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- PATCH /professor-survey/admin/questions/{questionId}/status ----

    @Test
    void updateQuestionStatus_asHead_success() throws Exception {
        when(professorSurveyService.updateQuestionStatus(anyLong(), anyBoolean())).thenReturn(new ProfessorSurveyQuestionDTO());

        mockMvc.perform(patch("/professor-survey/admin/questions/{questionId}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankActive\":true}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());
    }

    @Test
    void updateQuestionStatus_missingBankActive_returns400() throws Exception {
        mockMvc.perform(patch("/professor-survey/admin/questions/{questionId}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe indicar el estado de la pregunta"));
    }

    @Test
    void updateQuestionStatus_notHead_returns403() throws Exception {
        mockMvc.perform(patch("/professor-survey/admin/questions/{questionId}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankActive\":true}")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuestionStatus_error_returns400() throws Exception {
        when(professorSurveyService.updateQuestionStatus(anyLong(), anyBoolean()))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(patch("/professor-survey/admin/questions/{questionId}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankActive\":true}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /professor-survey/current-config ----

    @Test
    void getCurrentConfigForMonitor_success() throws Exception {
        when(professorSurveyService.getCurrentConfig(any())).thenReturn(new ProfessorSurveyCurrentConfigDTO());

        mockMvc.perform(get("/professor-survey/current-config"))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentConfigForMonitor_withSemester() throws Exception {
        when(professorSurveyService.getCurrentConfig(eq("2026-1"))).thenReturn(new ProfessorSurveyCurrentConfigDTO());

        mockMvc.perform(get("/professor-survey/current-config")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentConfigForMonitor_error_returns400() throws Exception {
        when(professorSurveyService.getCurrentConfig(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/professor-survey/current-config"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /professor-survey/admin/current-config ----

    @Test
    void getCurrentConfigAdmin_asHead_success() throws Exception {
        when(professorSurveyService.getCurrentConfig(any())).thenReturn(new ProfessorSurveyCurrentConfigDTO());

        mockMvc.perform(get("/professor-survey/admin/current-config")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentConfigAdmin_notHead_returns403() throws Exception {
        mockMvc.perform(get("/professor-survey/admin/current-config")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCurrentConfigAdmin_error_returns400() throws Exception {
        when(professorSurveyService.getCurrentConfig(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/professor-survey/admin/current-config")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /professor-survey/admin/validate-period ----

    @Test
    void validatePeriod_asHead_valid() throws Exception {
        mockMvc.perform(get("/professor-survey/admin/validate-period")
                        .param("semester", "2026-1")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validatePeriod_notHead_returns403() throws Exception {
        mockMvc.perform(get("/professor-survey/admin/validate-period")
                        .param("semester", "2026-1")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validatePeriod_invalidSemester_returns400() throws Exception {
        mockMvc.perform(get("/professor-survey/admin/validate-period")
                        .param("semester", "")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /professor-survey/admin/current-config ----

    @Test
    void saveCurrentConfig_asHead_success() throws Exception {
        when(professorSurveyService.saveCurrentConfig(any())).thenReturn(new ProfessorSurveyCurrentConfigDTO());

        mockMvc.perform(put("/professor-survey/admin/current-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());
    }

    @Test
    void saveCurrentConfig_notHead_returns403() throws Exception {
        mockMvc.perform(put("/professor-survey/admin/current-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveCurrentConfig_error_returns400() throws Exception {
        when(professorSurveyService.saveCurrentConfig(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(put("/professor-survey/admin/current-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /professor-survey/admin/templates ----

    @Test
    void listTemplates_asHead_success() throws Exception {
        when(professorSurveyService.listTemplates()).thenReturn(List.of(new ProfessorSurveyTemplateDTO()));

        mockMvc.perform(get("/professor-survey/admin/templates")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void listTemplates_notHead_returns403() throws Exception {
        mockMvc.perform(get("/professor-survey/admin/templates")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listTemplates_error_returns400() throws Exception {
        when(professorSurveyService.listTemplates()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/professor-survey/admin/templates")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /professor-survey/admin/templates ----

    @Test
    void createTemplate_asHead_success() throws Exception {
        when(professorSurveyService.createTemplate(any())).thenReturn(new ProfessorSurveyTemplateDTO());

        mockMvc.perform(post("/professor-survey/admin/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isCreated());
    }

    @Test
    void createTemplate_notHead_returns403() throws Exception {
        mockMvc.perform(post("/professor-survey/admin/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTemplate_error_returns400() throws Exception {
        when(professorSurveyService.createTemplate(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/professor-survey/admin/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /professor-survey/admin/templates/{templateId} ----

    @Test
    void updateTemplate_asHead_success() throws Exception {
        when(professorSurveyService.updateTemplate(anyLong(), any())).thenReturn(new ProfessorSurveyTemplateDTO());

        mockMvc.perform(put("/professor-survey/admin/templates/{templateId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());
    }

    @Test
    void updateTemplate_notHead_returns403() throws Exception {
        mockMvc.perform(put("/professor-survey/admin/templates/{templateId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTemplate_error_returns400() throws Exception {
        when(professorSurveyService.updateTemplate(anyLong(), any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(put("/professor-survey/admin/templates/{templateId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- DELETE /professor-survey/admin/templates/{templateId} ----

    @Test
    void deleteTemplate_asHead_success() throws Exception {
        mockMvc.perform(delete("/professor-survey/admin/templates/{templateId}", 1L)
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Plantilla eliminada"));
    }

    @Test
    void deleteTemplate_notHead_returns403() throws Exception {
        mockMvc.perform(delete("/professor-survey/admin/templates/{templateId}", 1L)
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTemplate_error_returns400() throws Exception {
        doThrow(new RuntimeException("Error")).when(professorSurveyService).deleteTemplate(1L);

        mockMvc.perform(delete("/professor-survey/admin/templates/{templateId}", 1L)
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /professor-survey/admin/apply-template ----

    @Test
    void applyTemplate_asHead_success() throws Exception {
        when(professorSurveyService.applyTemplate(any())).thenReturn(new ProfessorSurveyCurrentConfigDTO());

        mockMvc.perform(post("/professor-survey/admin/apply-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());
    }

    @Test
    void applyTemplate_notHead_returns403() throws Exception {
        mockMvc.perform(post("/professor-survey/admin/apply-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyTemplate_error_returns400() throws Exception {
        when(professorSurveyService.applyTemplate(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/professor-survey/admin/apply-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }
}
