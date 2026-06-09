package com.pdg.sigma;

import com.pdg.sigma.controller.MonitoringMonitorController;
import com.pdg.sigma.dto.MonitorDTO;
import com.pdg.sigma.dto.UpdateSelectionStatusRequest;
import com.pdg.sigma.service.MonitoringMonitorServiceImpl;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import jakarta.persistence.EntityNotFoundException;
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

@WebMvcTest(controllers = MonitoringMonitorController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitoringMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringMonitorServiceImpl monitoringMonitorService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getMonitorsByMonitoring_returnsList() throws Exception {
        MonitorDTO dto = new MonitorDTO();
        dto.setCode("M001");
        when(monitoringMonitorService.getMonitorsByMonitoringId(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/monitoring-monitor/{monitoringId}/monitors", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("M001"));
    }

    @Test
    void deleteMonitorRelation_returnsOk() throws Exception {
        doNothing().when(monitoringMonitorService).deleteRelation(1L, "M001");

        mockMvc.perform(delete("/monitoring-monitor/{idMonitoring}/{idMonitor}", 1L, "M001"))
                .andExpect(status().isOk())
                .andExpect(content().string("Relación eliminada exitosamente"));
    }

    @Test
    void deleteMonitorRelation_throws_returns500() throws Exception {
        doThrow(new RuntimeException("Error")).when(monitoringMonitorService).deleteRelation(1L, "M001");

        mockMvc.perform(delete("/monitoring-monitor/{idMonitoring}/{idMonitor}", 1L, "M001"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error: Error"));
    }

    @Test
    void updateSelectionStatus_returnsOk() throws Exception {
        doNothing().when(monitoringMonitorService)
                .updateApplicantSelectionStatus(1L, "M001", "ACCEPTED");

        mockMvc.perform(put("/monitoring-monitor/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\",\"newStatus\":\"ACCEPTED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateSelectionStatus_entityNotFound_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Not found")).when(monitoringMonitorService)
                .updateApplicantSelectionStatus(1L, "M001", "ACCEPTED");

        mockMvc.perform(put("/monitoring-monitor/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\",\"newStatus\":\"ACCEPTED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSelectionStatus_genericError_returns500() throws Exception {
        doThrow(new RuntimeException("DB Error")).when(monitoringMonitorService)
                .updateApplicantSelectionStatus(1L, "M001", "ACCEPTED");

        mockMvc.perform(put("/monitoring-monitor/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\",\"newStatus\":\"ACCEPTED\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error updating status: DB Error"));
    }
}
