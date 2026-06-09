package com.pdg.sigma;

import com.pdg.sigma.controller.DepartmentBudgetController;
import com.pdg.sigma.domain.DepartmentBudget;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.Program;
import com.pdg.sigma.repository.DepartmentBudgetRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.ProgramRepository;
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

@WebMvcTest(controllers = DepartmentBudgetController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class DepartmentBudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentBudgetRepository budgetRepo;

    @MockBean
    private ProgramRepository programRepo;

    @MockBean
    private MonitoringRepository monitoringRepo;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ---- GET /budget/{programName}/{semester} ----

    @Test
    void getBudget_success() throws Exception {
        Program program = new Program();
        program.setName("Ingeniería");
        when(programRepo.findByName("Ingeniería")).thenReturn(Optional.of(program));

        DepartmentBudget budget = new DepartmentBudget();
        budget.setTotalHours(100);
        when(budgetRepo.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(budget));

        Monitoring m1 = new Monitoring();
        m1.setSemester("2026-1");
        m1.setEstimatedHours(30);
        Monitoring m2 = new Monitoring();
        m2.setSemester("2026-1");
        m2.setEstimatedHours(20);
        when(monitoringRepo.findByProgram(program)).thenReturn(List.of(m1, m2));

        mockMvc.perform(get("/budget/{programName}/{semester}", "Ingeniería", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.program").value("Ingeniería"))
                .andExpect(jsonPath("$.semester").value("2026-1"))
                .andExpect(jsonPath("$.totalHours").value(100))
                .andExpect(jsonPath("$.usedHours").value(50))
                .andExpect(jsonPath("$.remainingHours").value(50));
    }

    @Test
    void getBudget_programNotFound_returns400() throws Exception {
        when(programRepo.findByName("Unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/budget/{programName}/{semester}", "Unknown", "2026-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Programa no encontrado"));
    }

    @Test
    void getBudget_budgetNotFound_returns404() throws Exception {
        Program program = new Program();
        program.setName("Ingeniería");
        when(programRepo.findByName("Ingeniería")).thenReturn(Optional.of(program));
        when(budgetRepo.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/budget/{programName}/{semester}", "Ingeniería", "2026-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay presupuesto configurado para este programa y semestre"));
    }

    @Test
    void getBudget_nullEstimatedHours_handlesZero() throws Exception {
        Program program = new Program();
        program.setName("Ingeniería");
        when(programRepo.findByName("Ingeniería")).thenReturn(Optional.of(program));

        DepartmentBudget budget = new DepartmentBudget();
        budget.setTotalHours(100);
        when(budgetRepo.findByProgramAndSemester(program, "2026-1"))
                .thenReturn(Optional.of(budget));

        Monitoring m = new Monitoring();
        m.setSemester("2026-1");
        m.setEstimatedHours(null);
        when(monitoringRepo.findByProgram(program)).thenReturn(List.of(m));

        mockMvc.perform(get("/budget/{programName}/{semester}", "Ingeniería", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedHours").value(0))
                .andExpect(jsonPath("$.remainingHours").value(100));
    }

    // ---- POST /budget/set ----

    @Test
    void setBudget_createNew_success() throws Exception {
        Program program = new Program();
        program.setName("Ingeniería");
        when(programRepo.findByName("Ingeniería")).thenReturn(Optional.of(program));
        when(budgetRepo.findByProgramAndSemester(program, "2026-1")).thenReturn(Optional.empty());

        mockMvc.perform(post("/budget/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programName\":\"Ingeniería\",\"semester\":\"2026-1\",\"totalHours\":80}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Presupuesto actualizado"));

        verify(budgetRepo).save(any(DepartmentBudget.class));
    }

    @Test
    void setBudget_updateExisting_success() throws Exception {
        Program program = new Program();
        program.setName("Ingeniería");
        when(programRepo.findByName("Ingeniería")).thenReturn(Optional.of(program));

        DepartmentBudget existing = new DepartmentBudget();
        existing.setTotalHours(50);
        when(budgetRepo.findByProgramAndSemester(program, "2026-1")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/budget/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programName\":\"Ingeniería\",\"semester\":\"2026-1\",\"totalHours\":120}"))
                .andExpect(status().isOk());

        verify(budgetRepo).save(argThat(b -> b.getTotalHours() == 120));
    }

    @Test
    void setBudget_invalidData_returns400() throws Exception {
        mockMvc.perform(post("/budget/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programName\":\"Ingeniería\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Datos inválidos para configurar presupuesto"));
    }

    @Test
    void setBudget_programNotFound_returns400() throws Exception {
        when(programRepo.findByName("Unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/budget/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programName\":\"Unknown\",\"semester\":\"2026-1\",\"totalHours\":80}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Programa no encontrado"));
    }
}
