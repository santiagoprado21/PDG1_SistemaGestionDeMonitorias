package com.pdg.sigma;

import com.pdg.sigma.controller.MonitorApplicationController;
import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitorApplicationDTO;
import com.pdg.sigma.dto.SelectMonitorRequest;
import com.pdg.sigma.service.MonitorApplicationService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MonitorApplicationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitorApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitorApplicationService monitorApplicationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MonitorApplication createSampleApplication(Long id) {
        Professor prof = new Professor();
        prof.setId("P001");
        prof.setName("Dr. Juan");

        Course course = new Course();
        course.setId(1L);
        course.setName("Programación I");

        MonitoringRequest request = new MonitoringRequest();
        request.setId(10L);
        request.setCourse(course);
        request.setProfessor(prof);
        request.setRequestedHours(10);
        request.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);

        Monitor monitor = new Monitor();
        monitor.setCode("M001");
        monitor.setIdMonitor("12345");
        monitor.setName("Carlos");
        monitor.setLastName("Pérez");
        monitor.setEmail("carlos@test.com");
        monitor.setSemester(5);
        monitor.setGradeAverage(4.5);
        monitor.setGradeCourse(4.0);

        MonitorApplication application = new MonitorApplication();
        application.setId(id);
        application.setMonitoringRequest(request);
        application.setMonitor(monitor);
        application.setMotivationLetter("Carta de motivación");
        application.setStatus(ApplicationStatus.POSTULADO);
        application.setApplicationDate(LocalDateTime.now());
        return application;
    }

    // ---- POST /monitor-application/apply ----

    @Test
    void applyToConvocatoria_returnsCreated() throws Exception {
        MonitorApplication app = createSampleApplication(1L);
        when(monitorApplicationService.applyToConvocatoria(any(MonitorApplicationDTO.class))).thenReturn(app);

        mockMvc.perform(post("/monitor-application/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringRequestId\":10,\"monitorId\":\"12345\",\"monitorCode\":\"M001\",\"motivationLetter\":\"Carta de motivación\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.monitorName").value("Carlos"));
    }

    @Test
    void applyToConvocatoria_throws_returns400() throws Exception {
        when(monitorApplicationService.applyToConvocatoria(any(MonitorApplicationDTO.class)))
                .thenThrow(new RuntimeException("Error al postular"));

        mockMvc.perform(post("/monitor-application/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringRequestId\":10,\"monitorId\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error al postular"));
    }

    // ---- GET /monitor-application/request/{requestId} ----

    @Test
    void getApplicationsByRequest_returnsList() throws Exception {
        when(monitorApplicationService.getApplicationsByRequest(10L))
                .thenReturn(List.of(createSampleApplication(1L)));

        mockMvc.perform(get("/monitor-application/request/{requestId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getApplicationsByRequest_throws_returns500() throws Exception {
        when(monitorApplicationService.getApplicationsByRequest(10L))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor-application/request/{requestId}", 10L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitor-application/monitor/{monitorId} ----

    @Test
    void getApplicationsByMonitor_returnsList() throws Exception {
        when(monitorApplicationService.getApplicationsByMonitor("12345"))
                .thenReturn(List.of(createSampleApplication(1L)));

        mockMvc.perform(get("/monitor-application/monitor/{monitorId}", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getApplicationsByMonitor_throws_returns500() throws Exception {
        when(monitorApplicationService.getApplicationsByMonitor("12345"))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor-application/monitor/{monitorId}", "12345"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- POST /monitor-application/select ----

    @Test
    void selectMonitor_success() throws Exception {
        doNothing().when(monitorApplicationService).selectMonitor(any(SelectMonitorRequest.class));

        mockMvc.perform(post("/monitor-application/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringRequestId\":10,\"applicationId\":1,\"professorId\":\"P001\",\"notes\":\"Buen candidato\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Monitor seleccionado exitosamente. La monitoría ha sido enviada para aprobación del jefe de departamento."));
    }

    @Test
    void selectMonitor_throws_returns400() throws Exception {
        doThrow(new RuntimeException("Error")).when(monitorApplicationService).selectMonitor(any(SelectMonitorRequest.class));

        mockMvc.perform(post("/monitor-application/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringRequestId\":10,\"applicationId\":1,\"professorId\":\"P001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- DELETE /monitor-application/{applicationId} ----

    @Test
    void cancelApplication_success() throws Exception {
        doNothing().when(monitorApplicationService).cancelApplication(1L, "12345");

        mockMvc.perform(delete("/monitor-application/{applicationId}", 1L)
                        .param("monitorId", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Postulación cancelada exitosamente"));
    }

    @Test
    void cancelApplication_throws_returns400() throws Exception {
        doThrow(new RuntimeException("Error")).when(monitorApplicationService).cancelApplication(1L, "12345");

        mockMvc.perform(delete("/monitor-application/{applicationId}", 1L)
                        .param("monitorId", "12345"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitor-application/check-applied/{requestId}/{monitorId} ----

    @Test
    void checkIfApplied_returnsTrue() throws Exception {
        when(monitorApplicationService.hasApplied(10L, "12345")).thenReturn(true);

        mockMvc.perform(get("/monitor-application/check-applied/{requestId}/{monitorId}", 10L, "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasApplied").value(true));
    }

    @Test
    void checkIfApplied_returnsFalse() throws Exception {
        when(monitorApplicationService.hasApplied(10L, "12345")).thenReturn(false);

        mockMvc.perform(get("/monitor-application/check-applied/{requestId}/{monitorId}", 10L, "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasApplied").value(false));
    }

    @Test
    void checkIfApplied_throws_returns500() throws Exception {
        when(monitorApplicationService.hasApplied(10L, "12345")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor-application/check-applied/{requestId}/{monitorId}", 10L, "12345"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitor-application/available/{monitorId}/{programId} ----

    @Test
    void getAvailableConvocatorias_returnsList() throws Exception {
        Professor prof = new Professor();
        prof.setName("Dr. Juan");
        Course course = new Course();
        course.setName("Programación I");
        MonitoringRequest request = new MonitoringRequest();
        request.setId(10L);
        request.setCourse(course);
        request.setProfessor(prof);
        request.setRequestedHours(10);
        request.setSemester("2025-1");
        request.setRequiredAverageGrade(3.5);
        request.setRequiredCourseGrade(3.0);

        when(monitorApplicationService.getAvailableConvocatoriasForMonitor("12345", 1))
                .thenReturn(List.of(request));

        mockMvc.perform(get("/monitor-application/available/{monitorId}/{programId}", "12345", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseName").value("Programación I"))
                .andExpect(jsonPath("$[0].professorName").value("Dr. Juan"))
                .andExpect(jsonPath("$[0].requestedHours").value(10))
                .andExpect(jsonPath("$[0].requiredAverageGrade").value(3.5));
    }

    @Test
    void getAvailableConvocatorias_throws_returns500() throws Exception {
        when(monitorApplicationService.getAvailableConvocatoriasForMonitor("12345", 1))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor-application/available/{monitorId}/{programId}", "12345", 1))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitor-application/{id} ----

    @Test
    void getApplicationById_returnsDTO() throws Exception {
        MonitorApplication app = createSampleApplication(1L);
        when(monitorApplicationService.findById(1L)).thenReturn(Optional.of(app));

        mockMvc.perform(get("/monitor-application/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.monitorName").value("Carlos"));
    }

    @Test
    void getApplicationById_notFound_returns404() throws Exception {
        when(monitorApplicationService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/monitor-application/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Postulación no encontrada"));
    }

    @Test
    void getApplicationById_throws_returns404() throws Exception {
        when(monitorApplicationService.findById(1L)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/monitor-application/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DB error"));
    }

    // ---- GET /monitor-application/all ----

    @Test
    void getAllApplications_returnsList() throws Exception {
        when(monitorApplicationService.findAll()).thenReturn(List.of(createSampleApplication(1L)));

        mockMvc.perform(get("/monitor-application/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAllApplications_throws_returns500() throws Exception {
        when(monitorApplicationService.findAll()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor-application/all"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }
}
