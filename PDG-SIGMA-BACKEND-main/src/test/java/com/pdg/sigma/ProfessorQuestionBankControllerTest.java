package com.pdg.sigma;

import com.pdg.sigma.controller.ProfessorQuestionBankController;
import com.pdg.sigma.dto.ProfessorSurveyQuestionCreateRequest;
import com.pdg.sigma.dto.ProfessorSurveyQuestionDTO;
import com.pdg.sigma.dto.ProfessorSurveyQuestionStatusRequest;
import com.pdg.sigma.dto.ProfessorSurveyQuestionUpdateRequest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfessorQuestionBankController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class ProfessorQuestionBankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfessorSurveyService professorSurveyService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllQuestions_asDepartmentHead_returnsOk() throws Exception {
        ProfessorSurveyQuestionDTO dto = new ProfessorSurveyQuestionDTO();
        dto.setId(1L);
        when(professorSurveyService.getQuestionBank("2026-1")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/preguntas-profesor")
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAllQuestions_nonDepartmentHead_returns403() throws Exception {
        mockMvc.perform(get("/api/preguntas-profesor")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void getAllQuestions_throws_returns400() throws Exception {
        when(professorSurveyService.getQuestionBank("2026-1")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/preguntas-profesor")
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    @Test
    void createQuestion_asDepartmentHead_returnsCreated() throws Exception {
        ProfessorSurveyQuestionDTO dto = new ProfessorSurveyQuestionDTO();
        dto.setId(1L);
        when(professorSurveyService.createQuestion(any(ProfessorSurveyQuestionCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/preguntas-profesor")
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"Question?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createQuestion_nonDepartmentHead_returns403() throws Exception {
        mockMvc.perform(post("/api/preguntas-profesor")
                        .requestAttr("role", "monitor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void updateQuestion_asDepartmentHead_returnsOk() throws Exception {
        ProfessorSurveyQuestionDTO dto = new ProfessorSurveyQuestionDTO();
        dto.setId(1L);
        when(professorSurveyService.updateQuestion(eq(1L), any(ProfessorSurveyQuestionUpdateRequest.class), eq("2026-1")))
                .thenReturn(dto);

        mockMvc.perform(put("/api/preguntas-profesor/{questionId}", 1L)
                        .requestAttr("role", "jfedpto")
                        .param("semester", "2026-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"Updated?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateQuestion_nonDepartmentHead_returns403() throws Exception {
        mockMvc.perform(put("/api/preguntas-profesor/{questionId}", 1L)
                        .requestAttr("role", "professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuestionStatus_asDepartmentHead_returnsOk() throws Exception {
        when(professorSurveyService.updateQuestionStatus(1L, true)).thenReturn(new ProfessorSurveyQuestionDTO());

        mockMvc.perform(patch("/api/preguntas-profesor/{questionId}/estado", 1L)
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankActive\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateQuestionStatus_nonDepartmentHead_returns403() throws Exception {
        mockMvc.perform(patch("/api/preguntas-profesor/{questionId}/estado", 1L)
                        .requestAttr("role", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankActive\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuestionStatus_missingBankActive_returns400() throws Exception {
        mockMvc.perform(patch("/api/preguntas-profesor/{questionId}/estado", 1L)
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe indicar el estado de la pregunta"));
    }

    @Test
    void updateQuestionStatus_throws_returns400() throws Exception {
        when(professorSurveyService.updateQuestionStatus(1L, true))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(patch("/api/preguntas-profesor/{questionId}/estado", 1L)
                        .requestAttr("role", "jfedpto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankActive\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }
}
