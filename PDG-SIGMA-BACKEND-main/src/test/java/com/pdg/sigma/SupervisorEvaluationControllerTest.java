package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.controller.SupervisorEvaluationController;
import com.pdg.sigma.dto.SupervisorEvaluationRequest;
import com.pdg.sigma.dto.SupervisorEvaluationResponse;
import com.pdg.sigma.dto.SupervisorEvaluationStatusDTO;
import com.pdg.sigma.service.SupervisorEvaluationService;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupervisorEvaluationService supervisorEvaluationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createEvaluation_returnsCreatedResponse() throws Exception {
        SupervisorEvaluationRequest request = new SupervisorEvaluationRequest();
        request.setMonitoringId(25L);
        request.setMonitorIdentifier("MON-10");
        request.setGuidanceClarity(6);
        request.setRoleExpectations(6);
        request.setAvailabilityDisposition(6);
        request.setSupportTimeliness(6);
        request.setFeedbackConstructive(6);
        request.setFeedbackFairness(6);
        request.setRespectfulTreatment(6);
        request.setTrustEnvironment(6);

        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(80L);
        response.setMonitoringId(25L);
        response.setPerformanceLevel("EXCELENTE");

        Mockito.when(supervisorEvaluationService.createEvaluation(eq("MON-10"), any(SupervisorEvaluationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/supervisor-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluationId", is(80)))
                .andExpect(jsonPath("$.performanceLevel", is("EXCELENTE")));
    }

    @Test
    void getAssignments_returnsList() throws Exception {
        SupervisorEvaluationStatusDTO dto = new SupervisorEvaluationStatusDTO();
        dto.setMonitoringId(40L);
        dto.setProfessorName("Professor One");
        dto.setStatus("PENDIENTE");
        dto.setEvaluated(false);

        Mockito.when(supervisorEvaluationService.getAssignmentsForMonitor("MON-10"))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/supervisor-evaluations/monitor/{id}/assignments", "MON-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDIENTE")));
    }

    @Test
    void getEvaluationsForCoordinator_forbiddenWhenRoleNotCoordinator() throws Exception {
        mockMvc.perform(get("/supervisor-evaluations/coordinator")
                        .requestAttr("role", "monitor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("No está autorizado")));
    }

    @Test
    void getEvaluationsForCoordinator_returnsData() throws Exception {
        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(90L);
        response.setPerformanceLevel("ADECUADO");

        Mockito.when(supervisorEvaluationService.getEvaluationsForCoordinator()).thenReturn(List.of(response));

        mockMvc.perform(get("/supervisor-evaluations/coordinator")
                        .requestAttr("role", "jfedpto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].evaluationId", is(90)));
    }
}
