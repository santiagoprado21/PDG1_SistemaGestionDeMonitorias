package com.pdg.sigma;

import com.pdg.sigma.controller.SupervisorEvaluationController;
import com.pdg.sigma.dto.SupervisorEvaluationRequest;
import com.pdg.sigma.dto.SupervisorEvaluationResponse;
import com.pdg.sigma.dto.SupervisorEvaluationStatusDTO;
import com.pdg.sigma.service.SupervisorEvaluationService;
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

@WebMvcTest(controllers = SupervisorEvaluationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class SupervisorEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupervisorEvaluationService supervisorEvaluationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String COORDINATOR_ROLE = "jfedpto";

    // ---- POST /supervisor-evaluations ----

    @Test
    void createEvaluation_returnsCreated() throws Exception {
        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(1L);
        when(supervisorEvaluationService.createEvaluation(anyString(), any(SupervisorEvaluationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/supervisor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitorIdentifier\":\"M001\",\"professorId\":\"P001\",\"grade\":4.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluationId").value(1));
    }

    @Test
    void createEvaluation_illegalState_returns409() throws Exception {
        when(supervisorEvaluationService.createEvaluation(anyString(), any(SupervisorEvaluationRequest.class)))
                .thenThrow(new IllegalStateException("Conflicto"));

        mockMvc.perform(post("/supervisor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitorIdentifier\":\"M001\",\"professorId\":\"P001\",\"grade\":4.5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto"));
    }

    @Test
    void createEvaluation_genericException_returns400() throws Exception {
        when(supervisorEvaluationService.createEvaluation(anyString(), any(SupervisorEvaluationRequest.class)))
                .thenThrow(new RuntimeException("Error genérico"));

        mockMvc.perform(post("/supervisor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitorIdentifier\":\"M001\",\"professorId\":\"P001\",\"grade\":4.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error genérico"));
    }

    // ---- GET /supervisor-evaluations/monitor/{monitorIdentifier}/assignments ----

    @Test
    void getAssignmentsForMonitor_returnsList() throws Exception {
        SupervisorEvaluationStatusDTO dto = new SupervisorEvaluationStatusDTO();
        dto.setStatus("PENDIENTE");
        when(supervisorEvaluationService.getAssignmentsForMonitor("M001"))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/supervisor-evaluations/monitor/{monitorIdentifier}/assignments", "M001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDIENTE"));
    }

    @Test
    void getAssignmentsForMonitor_notFound_returns404() throws Exception {
        when(supervisorEvaluationService.getAssignmentsForMonitor("M001"))
                .thenThrow(new RuntimeException("monitor no encontrado"));

        mockMvc.perform(get("/supervisor-evaluations/monitor/{monitorIdentifier}/assignments", "M001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("monitor no encontrado"));
    }

    @Test
    void getAssignmentsForMonitor_otherError_returns400() throws Exception {
        when(supervisorEvaluationService.getAssignmentsForMonitor("M001"))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/supervisor-evaluations/monitor/{monitorIdentifier}/assignments", "M001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /supervisor-evaluations/coordinator ----

    @Test
    void getAllEvaluations_asCoordinator_returnsList() throws Exception {
        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(1L);
        when(supervisorEvaluationService.getEvaluationsForCoordinator()).thenReturn(List.of(response));

        mockMvc.perform(get("/supervisor-evaluations/coordinator")
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationId").value(1));
    }

    @Test
    void getAllEvaluations_nonCoordinator_returns403() throws Exception {
        mockMvc.perform(get("/supervisor-evaluations/coordinator")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void getAllEvaluations_throws_returns400() throws Exception {
        when(supervisorEvaluationService.getEvaluationsForCoordinator())
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/supervisor-evaluations/coordinator")
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /supervisor-evaluations/coordinator/professor/{professorId} ----

    @Test
    void getEvaluationsByProfessor_asCoordinator_returnsList() throws Exception {
        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(1L);
        when(supervisorEvaluationService.getEvaluationsByProfessor("P001")).thenReturn(List.of(response));

        mockMvc.perform(get("/supervisor-evaluations/coordinator/professor/{professorId}", "P001")
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationId").value(1));
    }

    @Test
    void getEvaluationsByProfessor_nonCoordinator_returns403() throws Exception {
        mockMvc.perform(get("/supervisor-evaluations/coordinator/professor/{professorId}", "P001")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void getEvaluationsByProfessor_notFound_returns404() throws Exception {
        when(supervisorEvaluationService.getEvaluationsByProfessor("P001"))
                .thenThrow(new RuntimeException("profesor no encontrado"));

        mockMvc.perform(get("/supervisor-evaluations/coordinator/professor/{professorId}", "P001")
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("profesor no encontrado"));
    }

    @Test
    void getEvaluationsByProfessor_otherError_returns400() throws Exception {
        when(supervisorEvaluationService.getEvaluationsByProfessor("P001"))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/supervisor-evaluations/coordinator/professor/{professorId}", "P001")
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /supervisor-evaluations/coordinator/monitor/{monitorIdentifier} ----

    @Test
    void getEvaluationsByMonitor_asCoordinator_returnsList() throws Exception {
        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(1L);
        when(supervisorEvaluationService.getEvaluationsByMonitor("M001")).thenReturn(List.of(response));

        mockMvc.perform(get("/supervisor-evaluations/coordinator/monitor/{monitorIdentifier}", "M001")
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationId").value(1));
    }

    @Test
    void getEvaluationsByMonitor_nonCoordinator_returns403() throws Exception {
        mockMvc.perform(get("/supervisor-evaluations/coordinator/monitor/{monitorIdentifier}", "M001")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }

    @Test
    void getEvaluationsByMonitor_notFound_returns404() throws Exception {
        when(supervisorEvaluationService.getEvaluationsByMonitor("M001"))
                .thenThrow(new RuntimeException("monitor no encontrado"));

        mockMvc.perform(get("/supervisor-evaluations/coordinator/monitor/{monitorIdentifier}", "M001")
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("monitor no encontrado"));
    }

    @Test
    void getEvaluationsByMonitor_otherError_returns400() throws Exception {
        when(supervisorEvaluationService.getEvaluationsByMonitor("M001"))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/supervisor-evaluations/coordinator/monitor/{monitorIdentifier}", "M001")
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /supervisor-evaluations/{evaluationId} ----

    @Test
    void getEvaluation_asCoordinator_found_returnsDTO() throws Exception {
        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(1L);
        when(supervisorEvaluationService.getEvaluation(1L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/supervisor-evaluations/{evaluationId}", 1L)
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value(1));
    }

    @Test
    void getEvaluation_asCoordinator_notFound_returns404() throws Exception {
        when(supervisorEvaluationService.getEvaluation(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/supervisor-evaluations/{evaluationId}", 1L)
                        .requestAttr("role", COORDINATOR_ROLE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Evaluación no encontrada"));
    }

    @Test
    void getEvaluation_nonCoordinator_returns403() throws Exception {
        mockMvc.perform(get("/supervisor-evaluations/{evaluationId}", 1L)
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No está autorizado"));
    }
}
