package com.pdg.sigma;

import com.pdg.sigma.controller.DepartmentHeadController;
import com.pdg.sigma.domain.DepartmentHead;
import com.pdg.sigma.domain.HeadProgram;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.dto.ApproveApplicationRequest;
import com.pdg.sigma.dto.PendingApplicationDTO;
import com.pdg.sigma.repository.HeadProgramRepository;
import com.pdg.sigma.service.DepartmentHeadService;
import com.pdg.sigma.service.MonitoringMonitorService;
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

@WebMvcTest(controllers = DepartmentHeadController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class DepartmentHeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentHeadService departmentHeadService;

    @MockBean
    private MonitoringMonitorService monitoringMonitorService;

    @MockBean
    private HeadProgramRepository headProgramRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String ROLE_HEAD = "jfedpto";

    // ---- GET /department-head/getA ----

    @Test
    void getAllDepartmentHeads_returnsList() throws Exception {
        when(departmentHeadService.findAll()).thenReturn(List.of(new DepartmentHead()));

        mockMvc.perform(get("/department-head/getA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    // ---- GET /department-head/{id} ----

    @Test
    void getDepartmentHeadById_found() throws Exception {
        DepartmentHead dh = new DepartmentHead();
        dh.setId("1");
        when(departmentHeadService.findById(1)).thenReturn(Optional.of(dh));

        mockMvc.perform(get("/department-head/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void getDepartmentHeadById_notFound() throws Exception {
        when(departmentHeadService.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/department-head/{id}", 1))
                .andExpect(status().isNotFound());
    }

    // ---- POST /department-head/create ----

    @Test
    void createDepartmentHead_success() throws Exception {
        DepartmentHead dh = new DepartmentHead();
        dh.setId("1");
        when(departmentHeadService.save(any(DepartmentHead.class))).thenReturn(dh);

        mockMvc.perform(post("/department-head/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }

    // ---- PUT /department-head/update/{id} ----

    @Test
    void updateDepartmentHead_found_updates() throws Exception {
        DepartmentHead existing = new DepartmentHead();
        existing.setId("1");
        when(departmentHeadService.findById(1)).thenReturn(Optional.of(existing));
        when(departmentHeadService.save(any(DepartmentHead.class))).thenReturn(existing);

        mockMvc.perform(put("/department-head/update/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));

        verify(departmentHeadService).save(any(DepartmentHead.class));
    }

    @Test
    void updateDepartmentHead_notFound_returns404() throws Exception {
        when(departmentHeadService.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(put("/department-head/update/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\"}"))
                .andExpect(status().isNotFound());

        verify(departmentHeadService, never()).save(any());
    }

    // ---- DELETE /department-head/delete/{id} ----

    @Test
    void deleteDepartmentHead_found_deletes() throws Exception {
        when(departmentHeadService.findById(1)).thenReturn(Optional.of(new DepartmentHead()));

        mockMvc.perform(delete("/department-head/delete/{id}", 1))
                .andExpect(status().isNoContent());

        verify(departmentHeadService).deleteById(1);
    }

    @Test
    void deleteDepartmentHead_notFound_returns404() throws Exception {
        when(departmentHeadService.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/department-head/delete/{id}", 1))
                .andExpect(status().isNotFound());

        verify(departmentHeadService, never()).deleteById(any());
    }

    // ---- GET /department-head/{id}/program ----

    @Test
    void getPrograms_found() throws Exception {
        HeadProgram hp = new HeadProgram();
        when(headProgramRepository.findByDepartmentHeadId("1")).thenReturn(List.of(hp));

        mockMvc.perform(get("/department-head/{id}/program", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getPrograms_notFound() throws Exception {
        when(headProgramRepository.findByDepartmentHeadId("1")).thenReturn(List.of());

        mockMvc.perform(get("/department-head/{id}/program", 1))
                .andExpect(status().isNotFound());
    }

    // ---- GET /department-head/{id}/professors ----

    @Test
    void getProfessorsByDepartmentHead_success() throws Exception {
        Professor prof = new Professor("P001");
        when(departmentHeadService.getProfessorsByDepartmentHead("1")).thenReturn(List.of(prof));

        mockMvc.perform(get("/department-head/{id}/professors", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("P001"));
    }

    // ---- GET /department-head/profile/{id} ----

    @Test
    void getProfile_success() throws Exception {
        com.pdg.sigma.dto.DepartmentHeadDTO dto = new com.pdg.sigma.dto.DepartmentHeadDTO("Escuela", "Programa", "jfedpto", "Nombre");
        when(departmentHeadService.getProfile("1")).thenReturn(dto);

        mockMvc.perform(get("/department-head/profile/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.school").value("Escuela"));
    }

    @Test
    void getProfile_error_returns404() throws Exception {
        when(departmentHeadService.getProfile("1")).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/department-head/profile/{id}", "1"))
                .andExpect(status().isNotFound());
    }

    // ---- GET /department-head/{id}/pending-applications ----

    @Test
    void getPendingApplications_asHead_success() throws Exception {
        when(departmentHeadService.getPendingApplications("1"))
                .thenReturn(List.of(new PendingApplicationDTO()));

        mockMvc.perform(get("/department-head/{id}/pending-applications", "1")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getPendingApplications_notHead_returns403() throws Exception {
        mockMvc.perform(get("/department-head/{id}/pending-applications", "1")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPendingApplications_error_returns500() throws Exception {
        when(departmentHeadService.getPendingApplications("1"))
                .thenThrow(new RuntimeException("Error interno"));

        mockMvc.perform(get("/department-head/{id}/pending-applications", "1")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isInternalServerError());
    }

    // ---- POST /department-head/approve ----

    @Test
    void approveApplication_asHead_success() throws Exception {
        mockMvc.perform(post("/department-head/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\"}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());

        verify(monitoringMonitorService).approveApplication(any(ApproveApplicationRequest.class));
    }

    @Test
    void approveApplication_notHead_returns403() throws Exception {
        mockMvc.perform(post("/department-head/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\"}")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());

        verify(monitoringMonitorService, never()).approveApplication(any());
    }

    @Test
    void approveApplication_error_returns400() throws Exception {
        doThrow(new RuntimeException("Error")).when(monitoringMonitorService)
                .approveApplication(any(ApproveApplicationRequest.class));

        mockMvc.perform(post("/department-head/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\"}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /department-head/reject ----

    @Test
    void rejectApplication_asHead_success() throws Exception {
        mockMvc.perform(post("/department-head/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\"}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isOk());

        verify(monitoringMonitorService).rejectApplication(any(ApproveApplicationRequest.class));
    }

    @Test
    void rejectApplication_notHead_returns403() throws Exception {
        mockMvc.perform(post("/department-head/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\"}")
                        .requestAttr("role", "professor"))
                .andExpect(status().isForbidden());

        verify(monitoringMonitorService, never()).rejectApplication(any());
    }

    @Test
    void rejectApplication_error_returns400() throws Exception {
        doThrow(new RuntimeException("Error")).when(monitoringMonitorService)
                .rejectApplication(any(ApproveApplicationRequest.class));

        mockMvc.perform(post("/department-head/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monitoringId\":1,\"monitorCode\":\"M001\"}")
                        .requestAttr("role", ROLE_HEAD))
                .andExpect(status().isBadRequest());
    }
}
