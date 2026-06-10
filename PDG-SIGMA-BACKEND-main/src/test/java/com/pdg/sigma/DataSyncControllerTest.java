package com.pdg.sigma;

import com.pdg.sigma.controller.DataSyncController;
import com.pdg.sigma.dto.UpdateRequestDTO;
import com.pdg.sigma.service.DataSyncService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DataSyncController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class DataSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSyncService dataSyncService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void updateSystem_success() throws Exception {
        when(dataSyncService.syncData(any(UpdateRequestDTO.class))).thenReturn("OK");

        mockMvc.perform(post("/api/sync/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void updateSystem_exception_returns500() throws Exception {
        when(dataSyncService.syncData(any(UpdateRequestDTO.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/api/sync/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }
}
