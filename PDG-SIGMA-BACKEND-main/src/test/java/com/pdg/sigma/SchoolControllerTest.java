package com.pdg.sigma;

import com.pdg.sigma.controller.SchoolController;
import com.pdg.sigma.dto.SchoolDTO;
import com.pdg.sigma.service.SchoolServiceImpl;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SchoolController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class SchoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchoolServiceImpl schoolService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getSchools_found() throws Exception {
        when(schoolService.findAll()).thenReturn(List.of(new SchoolDTO(1L, "Ingeniería")));

        mockMvc.perform(get("/school/getSchools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ingeniería"));
    }

    @Test
    void getSchools_empty_returns400() throws Exception {
        when(schoolService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/school/getSchools"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se encontraron facultades"));
    }
}
