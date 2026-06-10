package com.pdg.sigma;

import com.pdg.sigma.controller.MonitoringController;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.dto.MonitoringDTO;
import com.pdg.sigma.service.MonitoringServiceImpl;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MonitoringController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringServiceImpl monitoringService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ---- POST /monitoring/create ----

    @Test
    void createMonitoring_success() throws Exception {
        Monitoring monitoring = new Monitoring();
        monitoring.setId(1L);
        when(monitoringService.save(any(MonitoringDTO.class))).thenReturn(monitoring);
        when(monitoringService.findById(1L)).thenReturn(Optional.of(monitoring));

        mockMvc.perform(post("/monitoring/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Se ha creado una monitoria"));
    }

    @Test
    void createMonitoring_notFoundAfterSave() throws Exception {
        Monitoring monitoring = new Monitoring();
        monitoring.setId(1L);
        when(monitoringService.save(any(MonitoringDTO.class))).thenReturn(monitoring);
        when(monitoringService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/monitoring/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se pudo crear la monitoria"));
    }

    @Test
    void createMonitoring_exception_returns400() throws Exception {
        when(monitoringService.save(any(MonitoringDTO.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitoring/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /monitoring/updateBudget/{id} ----

    @Test
    void updateBudget_success() throws Exception {
        mockMvc.perform(post("/monitoring/updateBudget/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estimatedHours\":10,\"hourlyRate\":5000}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Presupuesto actualizado"));

        verify(monitoringService).updateMonitoringBudget(1L, 10, 5000.0);
    }

    @Test
    void updateBudget_asStringNumbers_success() throws Exception {
        mockMvc.perform(post("/monitoring/updateBudget/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estimatedHours\":\"10\",\"hourlyRate\":\"5000\"}"))
                .andExpect(status().isOk());

        verify(monitoringService).updateMonitoringBudget(eq(1L), eq(10), eq(5000.0));
    }

    @Test
    void updateBudget_nullHoursAndRate() throws Exception {
        mockMvc.perform(post("/monitoring/updateBudget/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(monitoringService).updateMonitoringBudget(eq(1L), isNull(), isNull());
    }

    @Test
    void updateBudget_error_returns400() throws Exception {
        doThrow(new RuntimeException("Error")).when(monitoringService).updateMonitoringBudget(anyLong(), any(), any());

        mockMvc.perform(post("/monitoring/updateBudget/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estimatedHours\":10,\"hourlyRate\":5000}"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /monitoring/getA ----

    @Test
    void getAllMonitoring_nonEmpty() throws Exception {
        when(monitoringService.findAll()).thenReturn(List.of(new Monitoring()));

        mockMvc.perform(get("/monitoring/getA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getAllMonitoring_empty_returns400() throws Exception {
        when(monitoringService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/getA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No hay monitorias en la lista"));
    }

    @Test
    void getAllMonitoring_exception_returns500() throws Exception {
        when(monitoringService.findAll()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring/getA"))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /monitoring/getAllByProfessor/{id} ----

    @Test
    void getAllMonitoringByProfessor_nonEmpty() throws Exception {
        when(monitoringService.findAllByProfessor("P001")).thenReturn(List.of(new Monitoring()));

        mockMvc.perform(get("/monitoring/getAllByProfessor/{id}", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getAllMonitoringByProfessor_empty_returns200() throws Exception {
        when(monitoringService.findAllByProfessor("P001")).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/getAllByProfessor/{id}", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllMonitoringByProfessor_exception_returns500() throws Exception {
        when(monitoringService.findAllByProfessor("P001")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring/getAllByProfessor/{id}", "P001"))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /monitoring/getAllActiveByUserId/{userId}/{role} ----

    @Test
    void getMonitoringsByUserIdAndRole_professor() throws Exception {
        when(monitoringService.findMonitoringsByProfessorWithAssignedMonitors("P001"))
                .thenReturn(List.of(new Monitoring()));

        mockMvc.perform(get("/monitoring/getAllActiveByUserId/{userId}/{role}", "P001", "professor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getMonitoringsByUserIdAndRole_monitor() throws Exception {
        when(monitoringService.findMonitoringsByAssignedMonitor("M001"))
                .thenReturn(List.of(new Monitoring()));

        mockMvc.perform(get("/monitoring/getAllActiveByUserId/{userId}/{role}", "M001", "monitor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getMonitoringsByUserIdAndRole_empty_returns404() throws Exception {
        when(monitoringService.findMonitoringsByProfessorWithAssignedMonitors("P001"))
                .thenReturn(List.of());

        mockMvc.perform(get("/monitoring/getAllActiveByUserId/{userId}/{role}", "P001", "professor"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMonitoringsByUserIdAndRole_invalidRole_returns400() throws Exception {
        mockMvc.perform(get("/monitoring/getAllActiveByUserId/{userId}/{role}", "X001", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonitoringsByUserIdAndRole_blankUserId_returns400() throws Exception {
        mockMvc.perform(get("/monitoring/getAllActiveByUserId/{userId}/{role}", " ", "professor"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonitoringsByUserIdAndRole_illegalArgument_returns400() throws Exception {
        when(monitoringService.findMonitoringsByProfessorWithAssignedMonitors("P001"))
                .thenThrow(new IllegalArgumentException("Invalido"));

        mockMvc.perform(get("/monitoring/getAllActiveByUserId/{userId}/{role}", "P001", "professor"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonitoringsByUserIdAndRole_genericError_returns500() throws Exception {
        when(monitoringService.findMonitoringsByProfessorWithAssignedMonitors("P001"))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring/getAllActiveByUserId/{userId}/{role}", "P001", "professor"))
                .andExpect(status().isInternalServerError());
    }

    // ---- POST /monitoring/findByFaculty ----

    @Test
    void findByFaculty_found() throws Exception {
        when(monitoringService.findBySchool(any(MonitoringDTO.class))).thenReturn(List.of(new Monitoring()));

        mockMvc.perform(post("/monitoring/findByFaculty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void findByFaculty_empty_returns400() throws Exception {
        when(monitoringService.findBySchool(any(MonitoringDTO.class))).thenReturn(List.of());

        mockMvc.perform(post("/monitoring/findByFaculty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByFaculty_exception_returns500() throws Exception {
        when(monitoringService.findBySchool(any(MonitoringDTO.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitoring/findByFaculty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    // ---- POST /monitoring/findByProgram ----

    @Test
    void findByProgram_found() throws Exception {
        when(monitoringService.findByProgram(any(MonitoringDTO.class))).thenReturn(List.of(new Monitoring()));

        mockMvc.perform(post("/monitoring/findByProgram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void findByProgram_empty_returns400() throws Exception {
        when(monitoringService.findByProgram(any(MonitoringDTO.class))).thenReturn(List.of());

        mockMvc.perform(post("/monitoring/findByProgram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByProgram_exception_returns500() throws Exception {
        when(monitoringService.findByProgram(any(MonitoringDTO.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitoring/findByProgram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    // ---- POST /monitoring/findByCourse ----

    @Test
    void findByCourse_found() throws Exception {
        when(monitoringService.findByCourse(any(MonitoringDTO.class))).thenReturn(List.of(new Monitoring()));

        mockMvc.perform(post("/monitoring/findByCourse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void findByCourse_empty_returns400() throws Exception {
        when(monitoringService.findByCourse(any(MonitoringDTO.class))).thenReturn(List.of());

        mockMvc.perform(post("/monitoring/findByCourse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByCourse_exception_returns500() throws Exception {
        when(monitoringService.findByCourse(any(MonitoringDTO.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/monitoring/findByCourse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    // ---- POST /monitoring/createAll/{id} ----

    @Test
    void createMultipleMonitoring_success() throws Exception {
        when(monitoringService.processListMonitor(any(), eq("1"))).thenReturn("OK");

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        mockMvc.perform(multipart("/monitoring/createAll/{id}", "1").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void createMultipleMonitoring_exception_returns500() throws Exception {
        when(monitoringService.processListMonitor(any(), eq("1"))).thenThrow(new RuntimeException("Error"));

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        mockMvc.perform(multipart("/monitoring/createAll/{id}", "1").file(file))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /monitoring/profile/{id}/{role} ----

    @Test
    void profile_professor() throws Exception {
        when(monitoringService.getByProfessor("P001")).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/profile/{id}/{role}", "P001", "professor"))
                .andExpect(status().isOk());
    }

    @Test
    void profile_monitor() throws Exception {
        when(monitoringService.getByMonitor("M001")).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/profile/{id}/{role}", "M001", "monitor"))
                .andExpect(status().isOk());
    }

    @Test
    void profile_headDepartment() throws Exception {
        when(monitoringService.getByHeadDepartment("D001")).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/profile/{id}/{role}", "D001", "jfedpto"))
                .andExpect(status().isOk());
    }

    @Test
    void profile_exception_returns404() throws Exception {
        when(monitoringService.getByProfessor("P001")).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/monitoring/profile/{id}/{role}", "P001", "professor"))
                .andExpect(status().isNotFound());
    }

    // ---- GET /monitoring/getMonitorsReport/{idProfessor}/{role} ----

    @Test
    void getMonitorsReport_success() throws Exception {
        when(monitoringService.getReportMonitors("P001", "professor")).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/getMonitorsReport/{idProfessor}/{role}", "P001", "professor"))
                .andExpect(status().isOk());
    }

    @Test
    void getMonitorsReport_exception_returns500() throws Exception {
        when(monitoringService.getReportMonitors("P001", "professor")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring/getMonitorsReport/{idProfessor}/{role}", "P001", "professor"))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /monitoring/getProfessorReport/{idProfessor} ----

    @Test
    void getProfessorReport_success() throws Exception {
        when(monitoringService.getProfessorReport("P001")).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/getProfessorReport/{idProfessor}", "P001"))
                .andExpect(status().isOk());
    }

    @Test
    void getProfessorReport_exception_returns500() throws Exception {
        when(monitoringService.getProfessorReport("P001")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring/getProfessorReport/{idProfessor}", "P001"))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /monitoring/getCategoriesReport/professor/{professorId} ----

    @Test
    void getProfessorCategoriesReport_success() throws Exception {
        when(monitoringService.getCategoryReport(anyString(), any()))
                .thenReturn(Map.of("detalle_por_curso", List.of(), "totales_por_categoria", List.of()));

        mockMvc.perform(get("/monitoring/getCategoriesReport/professor/{professorId}", "P001"))
                .andExpect(status().isOk());
    }

    @Test
    void getProfessorCategoriesReport_notFound_returns404() throws Exception {
        when(monitoringService.getCategoryReport(anyString(), any()))
                .thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(get("/monitoring/getCategoriesReport/professor/{professorId}", "P001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfessorCategoriesReport_error_returns500() throws Exception {
        when(monitoringService.getCategoryReport(anyString(), any()))
                .thenThrow(new RuntimeException("Otro error"));

        mockMvc.perform(get("/monitoring/getCategoriesReport/professor/{professorId}", "P001"))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /monitoring/getCategoriesReport/jfedpto/{departmentHeadId} ----

    @Test
    void getDepartmentHeadCategoriesReport_success() throws Exception {
        when(monitoringService.getDepartmentCategoryReport(anyString(), any()))
                .thenReturn(Map.of("detalle_por_curso", List.of(), "totales_por_categoria", List.of()));

        mockMvc.perform(get("/monitoring/getCategoriesReport/jfedpto/{departmentHeadId}", "D001"))
                .andExpect(status().isOk());
    }

    @Test
    void getDepartmentHeadCategoriesReport_notFound_returns404() throws Exception {
        when(monitoringService.getDepartmentCategoryReport(anyString(), any()))
                .thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(get("/monitoring/getCategoriesReport/jfedpto/{departmentHeadId}", "D001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDepartmentHeadCategoriesReport_error_returns500() throws Exception {
        when(monitoringService.getDepartmentCategoryReport(anyString(), any()))
                .thenThrow(new RuntimeException("Otro error"));

        mockMvc.perform(get("/monitoring/getCategoriesReport/jfedpto/{departmentHeadId}", "D001"))
                .andExpect(status().isInternalServerError());
    }

    // ---- GET /monitoring/getAttendanceReport/{role}/{userId} ----

    @Test
    void getAttendanceReport_professor_success() throws Exception {
        when(monitoringService.getMonthlyAttendanceReport(anyString(), any())).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/getAttendanceReport/{role}/{userId}", "professor", "P001"))
                .andExpect(status().isOk());
    }

    @Test
    void getAttendanceReport_jfedpto_success() throws Exception {
        when(monitoringService.getDepartmentMonthlyAttendanceReport(anyString(), any())).thenReturn(List.of());

        mockMvc.perform(get("/monitoring/getAttendanceReport/{role}/{userId}", "jfedpto", "D001"))
                .andExpect(status().isOk());
    }

    @Test
    void getAttendanceReport_invalidRole_returns400() throws Exception {
        mockMvc.perform(get("/monitoring/getAttendanceReport/{role}/{userId}", "admin", "X001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAttendanceReport_notFound_returns404() throws Exception {
        when(monitoringService.getMonthlyAttendanceReport(anyString(), any()))
                .thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(get("/monitoring/getAttendanceReport/{role}/{userId}", "professor", "P001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAttendanceReport_error_returns500() throws Exception {
        when(monitoringService.getMonthlyAttendanceReport(anyString(), any()))
                .thenThrow(new RuntimeException("Interno"));

        mockMvc.perform(get("/monitoring/getAttendanceReport/{role}/{userId}", "professor", "P001"))
                .andExpect(status().isInternalServerError());
    }

    // ---- DELETE /monitoring/deleteMonitoring/{idMonitoring} ----

    @Test
    void deleteMonitoring_success() throws Exception {
        when(monitoringService.deleteMonitoring(1L)).thenReturn(true);

        mockMvc.perform(delete("/monitoring/deleteMonitoring/{idMonitoring}", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMonitoring_exception_returns500() throws Exception {
        when(monitoringService.deleteMonitoring(1L)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(delete("/monitoring/deleteMonitoring/{idMonitoring}", "1"))
                .andExpect(status().isInternalServerError());
    }

    // ---- POST /monitoring/approve/{monitoringId} ----

    @Test
    void approveMonitoring_success() throws Exception {
        mockMvc.perform(post("/monitoring/approve/{monitoringId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"D001\",\"comment\":\"Aprobado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Monitoría aprobada exitosamente"))
                .andExpect(jsonPath("$.monitoringId").value(1));

        verify(monitoringService).approveMonitoring(1L, "D001", "Aprobado");
    }

    @Test
    void approveMonitoring_missingDepartmentHeadId_returns400() throws Exception {
        mockMvc.perform(post("/monitoring/approve/{monitoringId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Aprobado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("departmentHeadId es requerido"));

        verify(monitoringService, never()).approveMonitoring(anyLong(), anyString(), anyString());
    }

    @Test
    void approveMonitoring_error_returns400() throws Exception {
        doThrow(new RuntimeException("Error al aprobar")).when(monitoringService)
                .approveMonitoring(anyLong(), anyString(), anyString());

        mockMvc.perform(post("/monitoring/approve/{monitoringId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"D001\",\"comment\":\"Aprobado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error al aprobar"));
    }

    // ---- POST /monitoring/reject/{monitoringId} ----

    @Test
    void rejectMonitoring_success() throws Exception {
        mockMvc.perform(post("/monitoring/reject/{monitoringId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"D001\",\"comment\":\"Rechazado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Monitoría rechazada"));

        verify(monitoringService).rejectMonitoring(1L, "D001", "Rechazado");
    }

    @Test
    void rejectMonitoring_missingDepartmentHeadId_returns400() throws Exception {
        mockMvc.perform(post("/monitoring/reject/{monitoringId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Rechazado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("departmentHeadId es requerido"));
    }

    @Test
    void rejectMonitoring_missingComment_returns400() throws Exception {
        mockMvc.perform(post("/monitoring/reject/{monitoringId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"D001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El comentario de rechazo es obligatorio"));
    }

    @Test
    void rejectMonitoring_error_returns400() throws Exception {
        doThrow(new RuntimeException("Error")).when(monitoringService)
                .rejectMonitoring(anyLong(), anyString(), anyString());

        mockMvc.perform(post("/monitoring/reject/{monitoringId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"D001\",\"comment\":\"Rechazado\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /monitoring/pending-approval ----

    @Test
    void getPendingApproval_success() throws Exception {
        when(monitoringService.findPendingApproval()).thenReturn(List.of(new Monitoring()));

        mockMvc.perform(get("/monitoring/pending-approval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getPendingApproval_error_returns500() throws Exception {
        when(monitoringService.findPendingApproval()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring/pending-approval"))
                .andExpect(status().isInternalServerError());
    }
}
