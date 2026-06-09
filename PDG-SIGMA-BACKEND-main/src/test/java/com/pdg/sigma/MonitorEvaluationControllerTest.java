package com.pdg.sigma;

import com.pdg.sigma.controller.MonitorEvaluationController;
import com.pdg.sigma.dto.MonitorEvaluationAssignmentDTO;
import com.pdg.sigma.dto.MonitorEvaluationRequest;
import com.pdg.sigma.dto.MonitorEvaluationResponse;
import com.pdg.sigma.service.MonitorEvaluationService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MonitorEvaluationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitorEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitorEvaluationService monitorEvaluationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ---- POST /monitor-evaluations ----

    @Test
    void createEvaluation_returnsCreated() throws Exception {
        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        response.setEvaluationId(1L);
        when(monitorEvaluationService.createEvaluation(anyString(), any(MonitorEvaluationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/monitor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\",\"monitoringId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluationId").value(1));
    }

    @Test
    void createEvaluation_conflict_returns409() throws Exception {
        when(monitorEvaluationService.createEvaluation(anyString(), any(MonitorEvaluationRequest.class)))
                .thenThrow(new IllegalStateException("Conflicto"));

        mockMvc.perform(post("/monitor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\",\"monitoringId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto"));
    }

    @Test
    void createEvaluation_error_returns400() throws Exception {
        when(monitorEvaluationService.createEvaluation(anyString(), any(MonitorEvaluationRequest.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\",\"monitoringId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- PUT /monitor-evaluations/{evaluationId} ----

    @Test
    void updateEvaluation_success() throws Exception {
        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        response.setEvaluationId(1L);
        when(monitorEvaluationService.updateEvaluation(anyLong(), anyString(), any(MonitorEvaluationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/monitor-evaluations/{evaluationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\",\"monitoringId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value(1));
    }

    @Test
    void updateEvaluation_forbidden_returns403() throws Exception {
        when(monitorEvaluationService.updateEvaluation(anyLong(), anyString(), any(MonitorEvaluationRequest.class)))
                .thenThrow(new RuntimeException("no está autorizado"));

        mockMvc.perform(put("/monitor-evaluations/{evaluationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\",\"monitoringId\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("no está autorizado"));
    }

    @Test
    void updateEvaluation_error_returns400() throws Exception {
        when(monitorEvaluationService.updateEvaluation(anyLong(), anyString(), any(MonitorEvaluationRequest.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(put("/monitor-evaluations/{evaluationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\",\"monitoringId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitor-evaluations/professor/{professorId} ----

    @Test
    void getEvaluationsByProfessor_success() throws Exception {
        when(monitorEvaluationService.getEvaluationsByProfessor("P001"))
                .thenReturn(List.of(new MonitorEvaluationResponse()));

        mockMvc.perform(get("/monitor-evaluations/professor/{professorId}", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getEvaluationsByProfessor_notFound_returns404() throws Exception {
        when(monitorEvaluationService.getEvaluationsByProfessor("P001"))
                .thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(get("/monitor-evaluations/professor/{professorId}", "P001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("no encontrado"));
    }

    @Test
    void getEvaluationsByProfessor_error_returns400() throws Exception {
        when(monitorEvaluationService.getEvaluationsByProfessor("P001"))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor-evaluations/professor/{professorId}", "P001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitor-evaluations/professor/{professorId}/assignments ----

    @Test
    void getAssignments_success() throws Exception {
        when(monitorEvaluationService.getEvaluationAssignmentsForProfessor(eq("P001"), any()))
                .thenReturn(List.of(new MonitorEvaluationAssignmentDTO()));

        mockMvc.perform(get("/monitor-evaluations/professor/{professorId}/assignments", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getAssignments_withSearch_success() throws Exception {
        when(monitorEvaluationService.getEvaluationAssignmentsForProfessor(eq("P001"), eq(Optional.of("term"))))
                .thenReturn(List.of(new MonitorEvaluationAssignmentDTO()));

        mockMvc.perform(get("/monitor-evaluations/professor/{professorId}/assignments", "P001")
                        .param("search", "term"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getAssignments_notFound_returns404() throws Exception {
        when(monitorEvaluationService.getEvaluationAssignmentsForProfessor(eq("P001"), any()))
                .thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(get("/monitor-evaluations/professor/{professorId}/assignments", "P001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("no encontrado"));
    }

    @Test
    void getAssignments_error_returns400() throws Exception {
        when(monitorEvaluationService.getEvaluationAssignmentsForProfessor(eq("P001"), any()))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor-evaluations/professor/{professorId}/assignments", "P001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitor-evaluations/monitor/{monitorIdentifier} ----

    @Test
    void getEvaluationsForMonitor_success() throws Exception {
        when(monitorEvaluationService.getEvaluationsForMonitor("M001"))
                .thenReturn(List.of(new MonitorEvaluationResponse()));

        mockMvc.perform(get("/monitor-evaluations/monitor/{monitorIdentifier}", "M001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getEvaluationsForMonitor_notFound_returns404() throws Exception {
        when(monitorEvaluationService.getEvaluationsForMonitor("M001"))
                .thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(get("/monitor-evaluations/monitor/{monitorIdentifier}", "M001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("no encontrado"));
    }

    @Test
    void getEvaluationsForMonitor_error_returns400() throws Exception {
        when(monitorEvaluationService.getEvaluationsForMonitor("M001"))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor-evaluations/monitor/{monitorIdentifier}", "M001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- PATCH /monitor-evaluations/{evaluationId}/acknowledge ----

    @Test
    void acknowledgeEvaluation_success() throws Exception {
        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        response.setEvaluationId(1L);
        when(monitorEvaluationService.acknowledgeEvaluation(anyLong(), anyString()))
                .thenReturn(response);

        mockMvc.perform(patch("/monitor-evaluations/{evaluationId}/acknowledge", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitorIdentifier\":\"M001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value(1));
    }

    @Test
    void acknowledgeEvaluation_nullBody_usesNullMonitor() throws Exception {
        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        when(monitorEvaluationService.acknowledgeEvaluation(eq(1L), isNull()))
                .thenReturn(response);

        mockMvc.perform(patch("/monitor-evaluations/{evaluationId}/acknowledge", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void acknowledgeEvaluation_notFound_returns404() throws Exception {
        when(monitorEvaluationService.acknowledgeEvaluation(anyLong(), anyString()))
                .thenThrow(new RuntimeException("no encontrada"));

        mockMvc.perform(patch("/monitor-evaluations/{evaluationId}/acknowledge", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitorIdentifier\":\"M001\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("no encontrada"));
    }

    @Test
    void acknowledgeEvaluation_forbidden_returns403() throws Exception {
        when(monitorEvaluationService.acknowledgeEvaluation(anyLong(), anyString()))
                .thenThrow(new RuntimeException("no pertenece"));

        mockMvc.perform(patch("/monitor-evaluations/{evaluationId}/acknowledge", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitorIdentifier\":\"M001\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("no pertenece"));
    }

    @Test
    void acknowledgeEvaluation_error_returns400() throws Exception {
        when(monitorEvaluationService.acknowledgeEvaluation(anyLong(), anyString()))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(patch("/monitor-evaluations/{evaluationId}/acknowledge", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitorIdentifier\":\"M001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitor-evaluations/{evaluationId} ----

    @Test
    void getEvaluation_found_returnsDTO() throws Exception {
        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        response.setEvaluationId(1L);
        when(monitorEvaluationService.getEvaluation(1L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/monitor-evaluations/{evaluationId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value(1));
    }

    @Test
    void getEvaluation_notFound_returns404() throws Exception {
        when(monitorEvaluationService.getEvaluation(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/monitor-evaluations/{evaluationId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Evaluación no encontrada"));
    }
}
