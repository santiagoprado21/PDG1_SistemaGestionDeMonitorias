package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.controller.MonitorEvaluationController;
import com.pdg.sigma.dto.MonitorEvaluationAssignmentDTO;
import com.pdg.sigma.dto.MonitorEvaluationRequest;
import com.pdg.sigma.dto.MonitorEvaluationResponse;
import com.pdg.sigma.service.MonitorEvaluationService;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
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

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MonitorEvaluationService monitorEvaluationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createEvaluation_returnsCreatedResponse() throws Exception {
        MonitorEvaluationRequest request = new MonitorEvaluationRequest();
        request.setProfessorId("PROF-1");
        request.setMonitoringId(10L);
        request.setMonitorCode("MON-1");
        request.setTaskCompliance(4);
        request.setTimelyCommunication(4);
        request.setPlanFulfillment(4);
        request.setAttitude(4);
        request.setComments("Buen trabajo");

        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        response.setEvaluationId(90L);
        response.setMonitoringId(10L);
        response.setMonitorFullName("Ana Pérez");
        response.setTotalScore(4.0);
        response.setPerformanceLevel("ADECUADO");

        Mockito.when(monitorEvaluationService.createEvaluation(eq("PROF-1"), any(MonitorEvaluationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/monitor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluationId", is(90)))
                .andExpect(jsonPath("$.monitorFullName", is("Ana Pérez")))
                .andExpect(jsonPath("$.performanceLevel", is("ADECUADO")));
    }

    @Test
    void createEvaluation_conflictWhenAlreadyExists() throws Exception {
        MonitorEvaluationRequest request = new MonitorEvaluationRequest();
        request.setProfessorId("PROF-1");
        request.setMonitoringId(10L);
        request.setMonitorCode("MON-1");

        Mockito.when(monitorEvaluationService.createEvaluation(eq("PROF-1"), any(MonitorEvaluationRequest.class)))
                .thenThrow(new IllegalStateException("Ya existe una evaluación"));

        mockMvc.perform(post("/monitor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Ya existe una evaluación")));
    }

    @Test
    void updateEvaluation_returnsOk() throws Exception {
        Long evaluationId = 44L;
        MonitorEvaluationRequest request = new MonitorEvaluationRequest();
        request.setProfessorId("PROF-2");
        request.setTaskCompliance(3);
        request.setTimelyCommunication(4);
        request.setPlanFulfillment(3);
        request.setAttitude(4);

        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        response.setEvaluationId(evaluationId);
        response.setTotalScore(3.5);
        response.setPerformanceLevel("DESTACADO");

        Mockito.when(monitorEvaluationService.updateEvaluation(eq(evaluationId), eq("PROF-2"), any(MonitorEvaluationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/monitor-evaluations/{id}", evaluationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId", is(44)))
                .andExpect(jsonPath("$.performanceLevel", is("DESTACADO")));
    }

    @Test
    void getAssignments_returnsList() throws Exception {
        MonitorEvaluationAssignmentDTO dto = new MonitorEvaluationAssignmentDTO();
        dto.setMonitoringId(5L);
        dto.setMonitorFullName("Carlos Díaz");
        dto.setEvaluated(true);
        dto.setTotalScore(4.25);
        dto.setPerformanceLevel("DESTACADO");

        Mockito.when(monitorEvaluationService.getEvaluationAssignmentsForProfessor(eq("PROF-3"), ArgumentMatchers.<Optional<String>>any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/monitor-evaluations/professor/{professorId}/assignments", "PROF-3")
                        .param("search", "carlos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].monitorFullName", is("Carlos Díaz")))
                .andExpect(jsonPath("$[0].evaluated", is(true)))
                .andExpect(jsonPath("$[0].totalScore", is(4.25)));
    }

    @Test
    void acknowledgeEvaluation_forbiddenWhenDoesNotBelong() throws Exception {
        Mockito.when(monitorEvaluationService.acknowledgeEvaluation(eq(70L), anyString()))
                .thenThrow(new Exception("Esta evaluación no pertenece al monitor indicado"));

        mockMvc.perform(patch("/monitor-evaluations/{id}/acknowledge", 70L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("monitorIdentifier", "MON-9"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Esta evaluación no pertenece al monitor indicado")));
    }

    @Test
    void getEvaluation_notFoundWhenMissing() throws Exception {
        Mockito.when(monitorEvaluationService.getEvaluation(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/monitor-evaluations/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Evaluación no encontrada")));
    }

    @Test
    void getEvaluationsForMonitor_returnsData() throws Exception {
        MonitorEvaluationResponse response = new MonitorEvaluationResponse();
        response.setEvaluationId(501L);
        response.setMonitoringName("Algoritmos - 2025-1");
        response.setCourseName("Algoritmos");
        response.setSemester("2025-1");
        response.setTotalScore(3.75);
        response.setPerformanceLevel("ADECUADO");
        response.setAcknowledgedByMonitor(false);
        response.setCreatedAt(LocalDateTime.now());

        Mockito.when(monitorEvaluationService.getEvaluationsForMonitor("MONITOR-1"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/monitor-evaluations/monitor/{identifier}", "MONITOR-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].evaluationId", is(501)))
                .andExpect(jsonPath("$[0].performanceLevel", is("ADECUADO")));
    }
}
