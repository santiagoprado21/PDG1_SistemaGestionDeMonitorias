package com.pdg.sigma;

import com.pdg.sigma.controller.MonitoringRequestController;
import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitoringRequestDTO;
import com.pdg.sigma.service.MonitoringRequestService;
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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MonitoringRequestController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MonitoringRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringRequestService monitoringRequestService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MonitoringRequest createSampleRequest(Long id) {
        Professor prof = new Professor();
        prof.setId("P001");
        prof.setName("Dr. Juan");

        Course course = new Course();
        course.setId(1L);
        course.setName("Programación I");

        School school = new School();
        school.setId(1L);
        school.setName("Facultad de Ingeniería");

        Program program = new Program();
        program.setId(1L);
        program.setName("Ingeniería de Sistemas");

        MonitoringRequest request = new MonitoringRequest();
        request.setId(id);
        request.setProfessor(prof);
        request.setCourse(course);
        request.setSchool(school);
        request.setProgram(program);
        request.setRequestedHours(10);
        request.setJustification("Justificación");
        request.setSemester("2025-1");
        request.setStartDate(new Date());
        request.setFinishDate(new Date());
        request.setRequiredAverageGrade(4.0);
        request.setRequiredCourseGrade(3.5);
        request.setHourlyRate(5000.0);
        request.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    // ---- POST /monitoring-request/create ----

    @Test
    void createConvocatoria_returnsCreated() throws Exception {
        MonitoringRequest request = createSampleRequest(1L);
        when(monitoringRequestService.createConvocatoria(any(MonitoringRequestDTO.class))).thenReturn(request);

        mockMvc.perform(post("/monitoring-request/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\",\"courseId\":1,\"schoolId\":1,\"programId\":1,\"requestedHours\":10,\"justification\":\"Justificación\",\"semester\":\"2025-1\",\"startDate\":\"2025-02-01T00:00:00\",\"finishDate\":\"2025-06-30T00:00:00\",\"requiredAverageGrade\":4.0,\"requiredCourseGrade\":3.5,\"hourlyRate\":5000.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.professorName").value("Dr. Juan"))
                .andExpect(jsonPath("$.courseName").value("Programación I"));
    }

    @Test
    void createConvocatoria_throws_returns400() throws Exception {
        when(monitoringRequestService.createConvocatoria(any(MonitoringRequestDTO.class)))
                .thenThrow(new RuntimeException("Error al crear"));

        mockMvc.perform(post("/monitoring-request/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\",\"courseId\":1,\"schoolId\":1,\"programId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error al crear"));
    }

    // ---- GET /monitoring-request/open ----

    @Test
    void getOpenConvocatorias_returnsList() throws Exception {
        when(monitoringRequestService.findOpenConvocatorias()).thenReturn(List.of(createSampleRequest(1L)));

        mockMvc.perform(get("/monitoring-request/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getOpenConvocatorias_throws_returns500() throws Exception {
        when(monitoringRequestService.findOpenConvocatorias()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/monitoring-request/open"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("DB error"));
    }

    // ---- GET /monitoring-request/open/program/{programId} ----

    @Test
    void getOpenConvocatoriasByProgram_returnsList() throws Exception {
        when(monitoringRequestService.findOpenConvocatoriasByProgram(1)).thenReturn(List.of(createSampleRequest(1L)));

        mockMvc.perform(get("/monitoring-request/open/program/{programId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getOpenConvocatoriasByProgram_throws_returns500() throws Exception {
        when(monitoringRequestService.findOpenConvocatoriasByProgram(1)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring-request/open/program/{programId}", 1))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitoring-request/professor/{professorId} ----

    @Test
    void getConvocatoriasByProfessor_returnsList() throws Exception {
        MonitoringRequest request = createSampleRequest(1L);
        when(monitoringRequestService.findByProfessor("P001")).thenReturn(List.of(request));
        when(monitoringRequestService.getApplicationCount(1L)).thenReturn(3);

        mockMvc.perform(get("/monitoring-request/professor/{professorId}", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].applicationCount").value(3));
    }

    @Test
    void getConvocatoriasByProfessor_throws_returns500() throws Exception {
        when(monitoringRequestService.findByProfessor("P001")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring-request/professor/{professorId}", "P001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitoring-request/pending-approval/department-head/{departmentHeadId} ----

    @Test
    void getPendingApprovalForDepartmentHead_returnsList() throws Exception {
        when(monitoringRequestService.findPendingApprovalForDepartmentHead("H001")).thenReturn(List.of(createSampleRequest(1L)));

        mockMvc.perform(get("/monitoring-request/pending-approval/department-head/{departmentHeadId}", "H001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getPendingApprovalForDepartmentHead_withMonitor_returnsName() throws Exception {
        MonitoringRequest request = createSampleRequest(1L);
        Monitor monitor = new Monitor();
        monitor.setName("Carlos");
        monitor.setLastName("Pérez");
        Monitoring monitoring = new Monitoring();
        monitoring.setAssignedMonitor(monitor);
        request.setCreatedMonitoring(monitoring);

        when(monitoringRequestService.findPendingApprovalForDepartmentHead("H001")).thenReturn(List.of(request));

        mockMvc.perform(get("/monitoring-request/pending-approval/department-head/{departmentHeadId}", "H001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].selectedMonitorName").value("Carlos Pérez"));
    }

    @Test
    void getPendingApprovalForDepartmentHead_throws_returns500() throws Exception {
        when(monitoringRequestService.findPendingApprovalForDepartmentHead("H001")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring-request/pending-approval/department-head/{departmentHeadId}", "H001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- GET /monitoring-request/{id} ----

    @Test
    void getConvocatoriaById_returnsDTO() throws Exception {
        MonitoringRequest request = createSampleRequest(1L);
        when(monitoringRequestService.findById(1L)).thenReturn(Optional.of(request));
        when(monitoringRequestService.getApplicationCount(1L)).thenReturn(5);

        mockMvc.perform(get("/monitoring-request/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.applicationCount").value(5));
    }

    @Test
    void getConvocatoriaById_notFound_returns404() throws Exception {
        when(monitoringRequestService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/monitoring-request/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Convocatoria no encontrada"));
    }

    @Test
    void getConvocatoriaById_throws_returns404() throws Exception {
        when(monitoringRequestService.findById(1L)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/monitoring-request/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DB error"));
    }

    // ---- POST /monitoring-request/{id}/cancel ----

    @Test
    void cancelConvocatoria_success() throws Exception {
        doNothing().when(monitoringRequestService).cancelConvocatoria(1L, "P001");

        mockMvc.perform(post("/monitoring-request/{id}/cancel", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Convocatoria cancelada exitosamente"));
    }

    @Test
    void cancelConvocatoria_missingProfessorId_returns400() throws Exception {
        mockMvc.perform(post("/monitoring-request/{id}/cancel", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("professorId es requerido"));
    }

    @Test
    void cancelConvocatoria_throws_returns400() throws Exception {
        doThrow(new RuntimeException("No se puede cancelar")).when(monitoringRequestService).cancelConvocatoria(1L, "P001");

        mockMvc.perform(post("/monitoring-request/{id}/cancel", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professorId\":\"P001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede cancelar"));
    }

    // ---- GET /monitoring-request/all ----

    @Test
    void getAllConvocatorias_returnsList() throws Exception {
        when(monitoringRequestService.findAll()).thenReturn(List.of(createSampleRequest(1L)));

        mockMvc.perform(get("/monitoring-request/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAllConvocatorias_throws_returns500() throws Exception {
        when(monitoringRequestService.findAll()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring-request/all"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- DELETE /monitoring-request/{id} ----

    @Test
    void deleteConvocatoria_success() throws Exception {
        doNothing().when(monitoringRequestService).deleteById(1L);

        mockMvc.perform(delete("/monitoring-request/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Convocatoria eliminada exitosamente"));
    }

    @Test
    void deleteConvocatoria_throws_returns400() throws Exception {
        doThrow(new RuntimeException("No se puede eliminar")).when(monitoringRequestService).deleteById(1L);

        mockMvc.perform(delete("/monitoring-request/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede eliminar"));
    }

    // ---- GET /monitoring-request/pending-head-approval/{departmentHeadId} ----

    @Test
    void getPendingHeadApproval_returnsList() throws Exception {
        when(monitoringRequestService.findPendingHeadApproval("H001")).thenReturn(List.of(createSampleRequest(1L)));

        mockMvc.perform(get("/monitoring-request/pending-head-approval/{departmentHeadId}", "H001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getPendingHeadApproval_throws_returns500() throws Exception {
        when(monitoringRequestService.findPendingHeadApproval("H001")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/monitoring-request/pending-head-approval/{departmentHeadId}", "H001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error"));
    }

    // ---- POST /monitoring-request/{id}/approve-by-head ----

    @Test
    void approveByHead_success() throws Exception {
        doNothing().when(monitoringRequestService).approveByHead(1L, "H001", "Aprobado");

        mockMvc.perform(post("/monitoring-request/{id}/approve-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\",\"comment\":\"Aprobado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Convocatoria aprobada exitosamente. Ahora está abierta para postulaciones."))
                .andExpect(jsonPath("$.newStatus").value("CONVOCATORIA_ABIERTA"));
    }

    @Test
    void approveByHead_missingDepartmentHeadId_returns400() throws Exception {
        mockMvc.perform(post("/monitoring-request/{id}/approve-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Aprobado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("departmentHeadId es requerido"));
    }

    @Test
    void approveByHead_emptyDepartmentHeadId_returns400() throws Exception {
        mockMvc.perform(post("/monitoring-request/{id}/approve-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"\",\"comment\":\"Aprobado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("departmentHeadId es requerido"));
    }

    @Test
    void approveByHead_missingComment_returns400() throws Exception {
        mockMvc.perform(post("/monitoring-request/{id}/approve-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El comentario es obligatorio"));
    }

    @Test
    void approveByHead_throws_returns400() throws Exception {
        doThrow(new RuntimeException("Error al aprobar")).when(monitoringRequestService).approveByHead(1L, "H001", "Aprobado");

        mockMvc.perform(post("/monitoring-request/{id}/approve-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\",\"comment\":\"Aprobado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error al aprobar"));
    }

    // ---- POST /monitoring-request/{id}/reject-by-head ----

    @Test
    void rejectByHead_success() throws Exception {
        doNothing().when(monitoringRequestService).rejectByHead(1L, "H001", "Rechazado");

        mockMvc.perform(post("/monitoring-request/{id}/reject-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\",\"comment\":\"Rechazado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Convocatoria rechazada exitosamente"))
                .andExpect(jsonPath("$.newStatus").value("RECHAZADA"));
    }

    @Test
    void rejectByHead_missingDepartmentHeadId_returns400() throws Exception {
        mockMvc.perform(post("/monitoring-request/{id}/reject-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Rechazado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("departmentHeadId es requerido"));
    }

    @Test
    void rejectByHead_missingComment_returns400() throws Exception {
        mockMvc.perform(post("/monitoring-request/{id}/reject-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El comentario es obligatorio"));
    }

    @Test
    void rejectByHead_throws_returns400() throws Exception {
        doThrow(new RuntimeException("Error al rechazar")).when(monitoringRequestService).rejectByHead(1L, "H001", "Rechazado");

        mockMvc.perform(post("/monitoring-request/{id}/reject-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\",\"comment\":\"Rechazado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error al rechazar"));
    }

    // ---- PUT /monitoring-request/{id}/modify-by-head ----

    @Test
    void modifyAndApproveByHead_success() throws Exception {
        MonitoringRequest updated = createSampleRequest(1L);
        when(monitoringRequestService.modifyAndApproveByHead(anyLong(), any(MonitoringRequestDTO.class), anyString(), anyString()))
                .thenReturn(updated);

        mockMvc.perform(put("/monitoring-request/{id}/modify-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\",\"comment\":\"Aprobado con cambios\",\"requestedHours\":15,\"hourlyRate\":6000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Convocatoria modificada y aprobada exitosamente"))
                .andExpect(jsonPath("$.newStatus").value("CONVOCATORIA_ABIERTA"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void modifyAndApproveByHead_missingDepartmentHeadId_returns400() throws Exception {
        mockMvc.perform(put("/monitoring-request/{id}/modify-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Aprobado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("departmentHeadId es requerido"));
    }

    @Test
    void modifyAndApproveByHead_missingComment_returns400() throws Exception {
        mockMvc.perform(put("/monitoring-request/{id}/modify-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El comentario es obligatorio"));
    }

    @Test
    void modifyAndApproveByHead_throws_returns400() throws Exception {
        when(monitoringRequestService.modifyAndApproveByHead(anyLong(), any(MonitoringRequestDTO.class), anyString(), anyString()))
                .thenThrow(new RuntimeException("Error al modificar"));

        mockMvc.perform(put("/monitoring-request/{id}/modify-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\",\"comment\":\"Aprobado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error al modificar"));
    }

    @Test
    void modifyAndApproveByHead_withAllFields_success() throws Exception {
        MonitoringRequest updated = createSampleRequest(1L);
        when(monitoringRequestService.modifyAndApproveByHead(anyLong(), any(MonitoringRequestDTO.class), anyString(), anyString()))
                .thenReturn(updated);

        mockMvc.perform(put("/monitoring-request/{id}/modify-by-head", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentHeadId\":\"H001\",\"comment\":\"Ok\",\"requestedHours\":20,\"justification\":\"Nueva justificación\",\"startDate\":\"2025-03-01\",\"finishDate\":\"2025-07-31\",\"requiredAverageGrade\":4.5,\"requiredCourseGrade\":4.0,\"hourlyRate\":5500.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
