package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.controller.DepartmentHeadController;
import com.pdg.sigma.dto.ApproveApplicationRequest;
import com.pdg.sigma.dto.PendingApplicationDTO;
import com.pdg.sigma.repository.HeadProgramRepository;
import com.pdg.sigma.service.DepartmentHeadServiceImpl;
import com.pdg.sigma.service.MonitoringMonitorServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.util.Arrays;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias para HU-003: Aprobación de Monitorías por Jefe de Departamento
 * Casos críticos: Aprobar, Rechazar, Seguridad, Validaciones, Listar pendientes
 */
@WebMvcTest(controllers = DepartmentHeadController.class)
@ComponentScan(basePackages = "com.pdg.sigma.util")
public class ApproveApplicationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentHeadServiceImpl departmentHeadServiceImpl;

    @MockBean
    private MonitoringMonitorServiceImpl monitoringMonitorService;

    @MockBean
    private HeadProgramRepository headProgramRepository;

    /**
     * Test 1: Aprobar postulación exitosamente
     */
    @Test
    @WithMockUser(roles = "jfedpto")
    public void testApproveApplication_Success() throws Exception {
        ApproveApplicationRequest request = new ApproveApplicationRequest(
                1L,
                "2024001",
                "El estudiante cumple con todos los requisitos",
                "12345"
        );

        doNothing().when(monitoringMonitorService).approveApplication(any(ApproveApplicationRequest.class));

        mockMvc.perform(MockMvcRequestBuilders.post("/department-head/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("role", "jfedpto")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Postulación aprobada exitosamente"));

        verify(monitoringMonitorService, times(1)).approveApplication(any(ApproveApplicationRequest.class));
    }

    /**
     * Test 2: Rechazar postulación exitosamente
     */
    @Test
    @WithMockUser(roles = "jfedpto")
    public void testRejectApplication_Success() throws Exception {
        ApproveApplicationRequest request = new ApproveApplicationRequest(
                1L,
                "2024001",
                "El estudiante no cumple con el requisito de promedio",
                "12345"
        );

        doNothing().when(monitoringMonitorService).rejectApplication(any(ApproveApplicationRequest.class));

        mockMvc.perform(MockMvcRequestBuilders.post("/department-head/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("role", "jfedpto")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Postulación rechazada exitosamente"));

        verify(monitoringMonitorService, times(1)).rejectApplication(any(ApproveApplicationRequest.class));
    }

    /**
     * Test 3: Acceso denegado por rol inválido al aprobar
     */
    @Test
    @WithMockUser(roles = "estudiante")
    public void testApproveApplication_AccessDenied_InvalidRole() throws Exception {
        ApproveApplicationRequest request = new ApproveApplicationRequest(
                1L,
                "2024001",
                "Comentario",
                "12345"
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/department-head/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("role", "estudiante")
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Acceso denegado. Solo jefes de departamento pueden aprobar postulaciones."));

        verify(monitoringMonitorService, never()).approveApplication(any());
    }

    /**
     * Test 4: Error al aprobar postulación ya procesada
     */
    @Test
    @WithMockUser(roles = "jfedpto")
    public void testApproveApplication_AlreadyProcessed() throws Exception {
        ApproveApplicationRequest request = new ApproveApplicationRequest(
                1L,
                "2024001",
                "Comentario",
                "12345"
        );

        doThrow(new Exception("Esta postulación ya fue procesada anteriormente"))
                .when(monitoringMonitorService).approveApplication(any(ApproveApplicationRequest.class));

        mockMvc.perform(MockMvcRequestBuilders.post("/department-head/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("role", "jfedpto")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Esta postulación ya fue procesada anteriormente"));
    }

    /**
     * Test 5: Obtener lista de postulaciones pendientes exitosamente
     */
    @Test
    @WithMockUser(roles = "jfedpto")
    public void testGetPendingApplications_Success() throws Exception {
        String departmentHeadId = "12345";
        
        List<PendingApplicationDTO> mockApplications = Arrays.asList(
                new PendingApplicationDTO(
                        1L, 1L, "Estructuras de Datos", "Juan Pérez", 
                        "María González", "2024001", "maria@mail.com",
                        4.5, 4.8, 5, "pendiente", null, null, null,
                        "Ingeniería de Sistemas", "Escuela de Ingeniería"
                ),
                new PendingApplicationDTO(
                        2L, 2L, "Programación II", "Ana López",
                        "Carlos Ruiz", "2024002", "carlos@mail.com",
                        4.2, 4.5, 6, "pendiente", null, null, null,
                        "Ingeniería de Sistemas", "Escuela de Ingeniería"
                )
        );

        when(departmentHeadServiceImpl.getPendingApplications(departmentHeadId))
                .thenReturn(mockApplications);

        mockMvc.perform(MockMvcRequestBuilders.get("/department-head/{id}/pending-applications", departmentHeadId)
                        .requestAttr("role", "jfedpto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].monitorName").value("María González"))
                .andExpect(jsonPath("$[0].monitorCode").value("2024001"))
                .andExpect(jsonPath("$[0].estadoSeleccion").value("pendiente"))
                .andExpect(jsonPath("$[1].monitorName").value("Carlos Ruiz"))
                .andExpect(jsonPath("$[1].monitorCode").value("2024002"));

        verify(departmentHeadServiceImpl, times(1)).getPendingApplications(departmentHeadId);
    }
}

