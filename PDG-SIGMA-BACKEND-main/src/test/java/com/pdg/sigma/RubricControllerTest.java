package com.pdg.sigma;

import com.pdg.sigma.controller.RubricController;
import com.pdg.sigma.dto.CreateRubricRequest;
import com.pdg.sigma.dto.RubricDTO;
import com.pdg.sigma.service.RubricService;
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

@WebMvcTest(controllers = RubricController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class RubricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RubricService rubricService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createRubric_returnsCreated() throws Exception {
        RubricDTO dto = new RubricDTO();
        dto.setId(1L);
        when(rubricService.createRubric(any(CreateRubricRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/rubric/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rubric 1\",\"professorId\":\"P001\",\"totalPoints\":100,\"criteria\":[{\"name\":\"Criterion 1\",\"maxScore\":50}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRubric_throws_returns400() throws Exception {
        when(rubricService.createRubric(any(CreateRubricRequest.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/api/rubric/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rubric 1\",\"professorId\":\"P001\",\"totalPoints\":100,\"criteria\":[{\"name\":\"Criterion 1\",\"maxScore\":50}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRubric_returnsOk() throws Exception {
        RubricDTO dto = new RubricDTO();
        dto.setId(1L);
        when(rubricService.updateRubric(eq(1L), any(CreateRubricRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/rubric/update/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"professorId\":\"P001\",\"totalPoints\":100,\"criteria\":[{\"name\":\"Criterion 1\",\"maxScore\":50}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateRubric_throws_returns400() throws Exception {
        when(rubricService.updateRubric(eq(1L), any(CreateRubricRequest.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(put("/api/rubric/update/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"professorId\":\"P001\",\"totalPoints\":100,\"criteria\":[{\"name\":\"Criterion 1\",\"maxScore\":50}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRubricById_found_returnsOk() throws Exception {
        RubricDTO dto = new RubricDTO();
        dto.setId(1L);
        when(rubricService.getRubricById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/rubric/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getRubricById_throws_returns404() throws Exception {
        when(rubricService.getRubricById(99L)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/rubric/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRubricsByProfessor_returnsOk() throws Exception {
        RubricDTO dto = new RubricDTO();
        dto.setId(1L);
        when(rubricService.getRubricsByProfessor("P001")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/rubric/professor/{professorId}", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getRubricsByProfessor_throws_returns500() throws Exception {
        when(rubricService.getRubricsByProfessor("P001")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/rubric/professor/{professorId}", "P001"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAllRubrics_returnsOk() throws Exception {
        RubricDTO dto = new RubricDTO();
        dto.setId(1L);
        when(rubricService.getAllRubrics()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/rubric/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAllRubrics_throws_returns500() throws Exception {
        when(rubricService.getAllRubrics()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/rubric/all"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void searchRubricsByName_returnsOk() throws Exception {
        RubricDTO dto = new RubricDTO();
        dto.setId(1L);
        when(rubricService.searchRubricsByName("Test")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/rubric/search")
                        .param("name", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void searchRubricsByName_throws_returns500() throws Exception {
        when(rubricService.searchRubricsByName("Test")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/rubric/search")
                        .param("name", "Test"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getRecentRubrics_returnsOk() throws Exception {
        RubricDTO dto = new RubricDTO();
        dto.setId(1L);
        when(rubricService.getRecentRubrics()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/rubric/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getRecentRubrics_throws_returns500() throws Exception {
        when(rubricService.getRecentRubrics()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/rubric/recent"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deleteRubric_returnsOk() throws Exception {
        doNothing().when(rubricService).deleteRubric(1L);

        mockMvc.perform(delete("/api/rubric/delete/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Rúbrica eliminada exitosamente"));
    }

    @Test
    void deleteRubric_throws_returns404() throws Exception {
        doThrow(new RuntimeException("Not found")).when(rubricService).deleteRubric(99L);

        mockMvc.perform(delete("/api/rubric/delete/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void existsByNameAndProfessor_returnsTrue() throws Exception {
        when(rubricService.existsByNameAndProfessor("Test", "P001")).thenReturn(true);

        mockMvc.perform(get("/api/rubric/exists")
                        .param("name", "Test")
                        .param("professorId", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void existsByNameAndProfessor_returnsFalse() throws Exception {
        when(rubricService.existsByNameAndProfessor("Test", "P001")).thenReturn(false);

        mockMvc.perform(get("/api/rubric/exists")
                        .param("name", "Test")
                        .param("professorId", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }
}
