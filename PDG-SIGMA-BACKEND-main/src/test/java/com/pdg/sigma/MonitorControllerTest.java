package com.pdg.sigma;

import com.pdg.sigma.controller.MonitorController;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.dto.MonitorDTO;
import com.pdg.sigma.service.MonitorServiceImpl;
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

@WebMvcTest(controllers = MonitorController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitorServiceImpl monitorService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ---- POST /monitor/create ----

    @Test
    void createMonitor_success() throws Exception {
        when(monitorService.saveNew(any(MonitorDTO.class))).thenReturn(new Monitor());

        mockMvc.perform(post("/monitor/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"M001\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Se ha creado una postulación"));
    }

    @Test
    void createMonitor_exception_returns400() throws Exception {
        when(monitorService.saveNew(any(MonitorDTO.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitor/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /monitor/getA ----

    @Test
    void getAllMonitor_returnsList() throws Exception {
        when(monitorService.findAllNew()).thenReturn(List.of(new MonitorDTO()));

        mockMvc.perform(get("/monitor/getA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getAllMonitor_exception_returns500() throws Exception {
        when(monitorService.findAllNew()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitor/getA"))
                .andExpect(status().isInternalServerError());
    }

    // ---- DELETE /monitor/{id} ----

    @Test
    void deleteMonitor_success() throws Exception {
        mockMvc.perform(delete("/monitor/{id}", "M001"))
                .andExpect(status().isOk())
                .andExpect(content().string("Candidature deleted successfully"));
    }

    @Test
    void deleteMonitor_exception_returns404() throws Exception {
        doThrow(new RuntimeException("No encontrado")).when(monitorService).deleteById("M001");

        mockMvc.perform(delete("/monitor/{id}", "M001"))
                .andExpect(status().isNotFound());
    }

    // ---- GET /monitor/profile/{id} ----

    @Test
    void profile_success() throws Exception {
        when(monitorService.getProfile("M001")).thenReturn(new MonitorDTO());

        mockMvc.perform(get("/monitor/profile/{id}", "M001"))
                .andExpect(status().isOk());
    }

    @Test
    void profile_exception_returns404() throws Exception {
        when(monitorService.getProfile("M001")).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/monitor/profile/{id}", "M001"))
                .andExpect(status().isNotFound());
    }
}
