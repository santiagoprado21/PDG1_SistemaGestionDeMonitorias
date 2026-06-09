package com.pdg.sigma;

import com.pdg.sigma.controller.ActivityScheduleController;
import com.pdg.sigma.dto.ActivityPlanDTO;
import com.pdg.sigma.dto.ActivityScheduleDTO;
import com.pdg.sigma.dto.ScheduleConflictDTO;
import com.pdg.sigma.service.ActivityScheduleService;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ActivityScheduleController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class ActivityScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityScheduleService activityScheduleService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    // ---- POST /api/activity-schedule/create ----

    @Test
    void createActivityWithSchedule_success() throws Exception {
        ActivityScheduleDTO dto = new ActivityScheduleDTO();
        dto.setId(1);
        when(activityScheduleService.saveActivityWithSchedule(any(ActivityScheduleDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/activity-schedule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Actividad 1\",\"finish\":\"2026-06-10T00:00:00.000+00:00\",\"roleCreator\":\"professor\",\"roleResponsable\":\"monitor\",\"description\":\"Descripción\",\"monitoringId\":1,\"state\":\"PENDIENTE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createActivityWithSchedule_error_returns400() throws Exception {
        when(activityScheduleService.saveActivityWithSchedule(any(ActivityScheduleDTO.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/api/activity-schedule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Actividad 1\",\"finish\":\"2026-06-10T00:00:00.000+00:00\",\"roleCreator\":\"professor\",\"roleResponsable\":\"monitor\",\"description\":\"Descripción\",\"monitoringId\":1,\"state\":\"PENDIENTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createActivityWithSchedule_validationError() throws Exception {
        mockMvc.perform(post("/api/activity-schedule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/activity-schedule/validate-conflicts ----

    @Test
    void validateScheduleConflicts_noConflicts() throws Exception {
        when(activityScheduleService.validateScheduleConflicts(any(ActivityScheduleDTO.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/activity-schedule/validate-conflicts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Actividad 1\",\"finish\":\"2026-06-10T00:00:00.000+00:00\",\"roleCreator\":\"professor\",\"roleResponsable\":\"monitor\",\"description\":\"Descripción\",\"monitoringId\":1,\"state\":\"PENDIENTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void validateScheduleConflicts_withConflicts() throws Exception {
        ScheduleConflictDTO conflict = new ScheduleConflictDTO();
        when(activityScheduleService.validateScheduleConflicts(any(ActivityScheduleDTO.class)))
                .thenReturn(List.of(conflict));

        mockMvc.perform(post("/api/activity-schedule/validate-conflicts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Actividad 1\",\"finish\":\"2026-06-10T00:00:00.000+00:00\",\"roleCreator\":\"professor\",\"roleResponsable\":\"monitor\",\"description\":\"Descripción\",\"monitoringId\":1,\"state\":\"PENDIENTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void validateScheduleConflicts_error_returns400() throws Exception {
        when(activityScheduleService.validateScheduleConflicts(any(ActivityScheduleDTO.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/api/activity-schedule/validate-conflicts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Actividad 1\",\"finish\":\"2026-06-10T00:00:00.000+00:00\",\"roleCreator\":\"professor\",\"roleResponsable\":\"monitor\",\"description\":\"Descripción\",\"monitoringId\":1,\"state\":\"PENDIENTE\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/activity-schedule/plan/{monitoringId} ----

    @Test
    void getActivityPlan_success() throws Exception {
        ActivityPlanDTO plan = new ActivityPlanDTO();
        when(activityScheduleService.getActivityPlan(1)).thenReturn(plan);

        mockMvc.perform(get("/api/activity-schedule/plan/{monitoringId}", 1))
                .andExpect(status().isOk());
    }

    @Test
    void getActivityPlan_notFound_returns404() throws Exception {
        when(activityScheduleService.getActivityPlan(1)).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/api/activity-schedule/plan/{monitoringId}", 1))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/activity-schedule/monitor/{monitorId} ----

    @Test
    void getMonitorSchedule_success() throws Exception {
        when(activityScheduleService.getMonitorSchedule(anyString(), any(Date.class), any(Date.class)))
                .thenReturn(List.of(new ActivityScheduleDTO()));

        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}", "M001")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getMonitorSchedule_error_returns500() throws Exception {
        when(activityScheduleService.getMonitorSchedule(anyString(), any(Date.class), any(Date.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}", "M001")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /api/activity-schedule/professor/{professorId} ----

    @Test
    void getProfessorSchedule_success() throws Exception {
        when(activityScheduleService.getProfessorSchedule(anyString(), any(Date.class), any(Date.class)))
                .thenReturn(List.of(new ActivityScheduleDTO()));

        mockMvc.perform(get("/api/activity-schedule/professor/{professorId}", "P001")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getProfessorSchedule_error_returns500() throws Exception {
        when(activityScheduleService.getProfessorSchedule(anyString(), any(Date.class), any(Date.class)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/activity-schedule/professor/{professorId}", "P001")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /api/activity-schedule/monitor/{monitorId}/all-plans ----

    @Test
    void getMonitorActivityPlans_success() throws Exception {
        when(activityScheduleService.getMonitorActivityPlans("M001")).thenReturn(List.of(new ActivityPlanDTO()));

        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", "M001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getMonitorActivityPlans_error_returns500() throws Exception {
        when(activityScheduleService.getMonitorActivityPlans("M001"))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/activity-schedule/monitor/{monitorId}/all-plans", "M001"))
                .andExpect(status().isInternalServerError());
    }
}
