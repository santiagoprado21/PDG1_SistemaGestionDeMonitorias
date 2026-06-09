package com.pdg.sigma;

import com.pdg.sigma.controller.MonitoringClosureController;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.dto.MonitoringClosureReport;
import com.pdg.sigma.dto.MonitoringClosureRequest;
import com.pdg.sigma.service.MonitoringClosureService;
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

@WebMvcTest(controllers = MonitoringClosureController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitoringClosureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringClosureService monitoringClosureService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getMonitoringsReadyForClosure_returnsOk() throws Exception {
        Monitoring m = new Monitoring();
        m.setId(1L);
        when(monitoringClosureService.getMonitoringsReadyForClosure("2026-1", 1)).thenReturn(List.of(m));

        mockMvc.perform(get("/monitoring-closure/ready-for-closure")
                        .param("semester", "2026-1")
                        .param("programId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getMonitoringsReadyForClosure_throws_returns500() throws Exception {
        when(monitoringClosureService.getMonitoringsReadyForClosure("2026-1", 1))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring-closure/ready-for-closure")
                        .param("semester", "2026-1")
                        .param("programId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    @Test
    void closeMonitoring_returnsOk() throws Exception {
        MonitoringClosureReport report = new MonitoringClosureReport();
        report.setMonitoringId(1L);
        when(monitoringClosureService.closeMonitoring(anyLong(), any(MonitoringClosureRequest.class), eq("H001")))
                .thenReturn(report);

        mockMvc.perform(post("/monitoring-closure/{id}/close", 1L)
                        .param("directorId", "H001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"OK\",\"autoCalculate\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitoringId").value(1));
    }

    @Test
    void closeMonitoring_throws_returns400() throws Exception {
        when(monitoringClosureService.closeMonitoring(anyLong(), any(MonitoringClosureRequest.class), eq("H001")))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitoring-closure/{id}/close", 1L)
                        .param("directorId", "H001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"OK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    @Test
    void closeMonitoringsBatch_returnsOk() throws Exception {
        MonitoringClosureReport report = new MonitoringClosureReport();
        report.setMonitoringId(1L);
        when(monitoringClosureService.closeMonitoringsBatch(anyList(), anyString(), any(MonitoringClosureRequest.class)))
                .thenReturn(List.of(report));

        mockMvc.perform(post("/monitoring-closure/close-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringIds\":[1,2],\"directorId\":\"H001\",\"closureData\":{\"comment\":\"Batch\",\"autoCalculate\":true}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Monitorías cerradas exitosamente"))
                .andExpect(jsonPath("$.closed").value(1));
    }

    @Test
    void closeMonitoringsBatch_noMonitoringIds_returns400() throws Exception {
        mockMvc.perform(post("/monitoring-closure/close-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"directorId\":\"H001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Se requiere al menos una monitoría para cerrar"));
    }

    @Test
    void closeMonitoringsBatch_noDirectorId_returns400() throws Exception {
        mockMvc.perform(post("/monitoring-closure/close-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringIds\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Se requiere el ID del director"));
    }

    @Test
    void closeMonitoringsBatch_throws_returns400() throws Exception {
        when(monitoringClosureService.closeMonitoringsBatch(anyList(), anyString(), any(MonitoringClosureRequest.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitoring-closure/close-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringIds\":[1],\"directorId\":\"H001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    @Test
    void getComplianceReport_returnsOk() throws Exception {
        MonitoringClosureReport report = new MonitoringClosureReport();
        report.setMonitoringId(1L);
        when(monitoringClosureService.generateComplianceReport(1L)).thenReturn(report);

        mockMvc.perform(get("/monitoring-closure/{id}/report", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitoringId").value(1));
    }

    @Test
    void getComplianceReport_throws_returns404() throws Exception {
        when(monitoringClosureService.generateComplianceReport(99L)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/monitoring-closure/{id}/report", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
    }

    @Test
    void getClosedMonitorings_returnsOk() throws Exception {
        Monitoring m = new Monitoring();
        m.setId(1L);
        when(monitoringClosureService.getClosedMonitorings("2026-1", 1)).thenReturn(List.of(m));

        mockMvc.perform(get("/monitoring-closure/closed")
                        .param("semester", "2026-1")
                        .param("programId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getClosedMonitorings_throws_returns500() throws Exception {
        when(monitoringClosureService.getClosedMonitorings("2026-1", 1))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring-closure/closed")
                        .param("semester", "2026-1")
                        .param("programId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }
}
