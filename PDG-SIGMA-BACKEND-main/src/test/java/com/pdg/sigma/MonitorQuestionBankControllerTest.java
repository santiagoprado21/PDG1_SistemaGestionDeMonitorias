package com.pdg.sigma;

import com.pdg.sigma.controller.MonitorQuestionBankController;
import com.pdg.sigma.dto.MonitorSurveyQuestionCreateRequest;
import com.pdg.sigma.dto.MonitorSurveyQuestionDTO;
import com.pdg.sigma.dto.MonitorSurveyQuestionUpdateRequest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MonitorQuestionBankController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitorQuestionBankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitorSurveyService monitorSurveyService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllQuestions_asDepartmentHead_returnsOk() throws Exception {
        MonitorSurveyQuestionDTO dto = new MonitorSurveyQuestionDTO();
        dto.setId(1L);
        when(monitorSurveyService.getQuestionBank("2026-1")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/preguntas-monitor")
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAllQuestions_nonDepartmentHead_returns403() throws Exception {
        mockMvc.perform(get("/api/preguntas-monitor")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void getAllQuestions_throws_returns400() throws Exception {
        when(monitorSurveyService.getQuestionBank("2026-1")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/preguntas-monitor")
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    @Test
    void createQuestion_asDepartmentHead_returnsCreated() throws Exception {
        MonitorSurveyQuestionDTO dto = new MonitorSurveyQuestionDTO();
        dto.setId(1L);
        when(monitorSurveyService.createQuestion(any(MonitorSurveyQuestionCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/preguntas-monitor")
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"Question?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createQuestion_nonDepartmentHead_returns403() throws Exception {
        mockMvc.perform(post("/api/preguntas-monitor")
                        .requestAttr("role", "monitor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void updateQuestion_asDepartmentHead_returnsOk() throws Exception {
        MonitorSurveyQuestionDTO dto = new MonitorSurveyQuestionDTO();
        dto.setId(1L);
        when(monitorSurveyService.updateQuestion(eq(1L), any(MonitorSurveyQuestionUpdateRequest.class), eq("2026-1")))
                .thenReturn(dto);

        mockMvc.perform(put("/api/preguntas-monitor/{questionId}", 1L)
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"Updated?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateQuestion_nonDepartmentHead_returns403() throws Exception {
        mockMvc.perform(put("/api/preguntas-monitor/{questionId}", 1L)
                        .requestAttr("role", "professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuestion_throws_returns400() throws Exception {
        when(monitorSurveyService.updateQuestion(eq(1L), any(MonitorSurveyQuestionUpdateRequest.class), eq("2026-1")))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(put("/api/preguntas-monitor/{questionId}", 1L)
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }
}
