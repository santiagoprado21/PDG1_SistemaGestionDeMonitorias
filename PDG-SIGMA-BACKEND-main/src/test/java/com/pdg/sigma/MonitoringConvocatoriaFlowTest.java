package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.controller.*;
import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para HU-010: Flujo de Convocatorias de Monitoría
 * Prueba los endpoints principales del flujo completo de convocatorias
 */
@WebMvcTest(controllers = {MonitoringRequestController.class, MonitorApplicationController.class})
@ComponentScan(basePackages = "com.pdg.sigma.util")
public class MonitoringConvocatoriaFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MonitoringRequestService monitoringRequestService;

    @MockBean
    private MonitorApplicationService monitorApplicationService;

    private MonitoringRequest mockRequest;
    private MonitorApplication mockApplication;

    @BeforeEach
    void setUp() {
        // Mock de MonitoringRequest
        mockRequest = new MonitoringRequest();
        mockRequest.setId(1L);
        mockRequest.setStatus(RequestStatus.CONVOCATORIA_ABIERTA);

        // Mock de MonitorApplication
        mockApplication = new MonitorApplication();
        mockApplication.setId(1L);
        mockApplication.setStatus(ApplicationStatus.POSTULADO);
    }

    /**
     * Test 1: Profesor crea convocatoria exitosamente
     */
    @Test
    @WithMockUser(roles = "professor")
    public void testCreateConvocatoria_Success() throws Exception {
        // Arrange
        MonitoringRequestDTO dto = new MonitoringRequestDTO();
        dto.setProfessorId("1001");
        dto.setCourseId(1L);
        dto.setSchoolId(1);
        dto.setProgramId(1L);
        dto.setRequestedHours(20);
        dto.setSemester("2025-1");
        dto.setStartDate(new Date());
        dto.setFinishDate(new Date());
        dto.setJustification("Se requiere monitor para apoyar en laboratorios de programación y tutorías grupales");
        dto.setRequiredAverageGrade(4.0);
        dto.setRequiredCourseGrade(4.0);

        when(monitoringRequestService.createConvocatoria(any(MonitoringRequestDTO.class)))
                .thenReturn(mockRequest);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/monitoring-request/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(monitoringRequestService, times(1)).createConvocatoria(any(MonitoringRequestDTO.class));
    }

    /**
     * Test 2: Estudiante se postula a convocatoria
     */
    @Test
    @WithMockUser(roles = "student")
    public void testApplyToConvocatoria_Success() throws Exception {
        // Arrange
        MonitorApplicationDTO dto = new MonitorApplicationDTO();
        dto.setMonitoringRequestId(1L);
        dto.setMonitorId("2220001");
        dto.setMotivationLetter("Estoy interesado en ser monitor porque tengo experiencia en programación Java y Python.");

        when(monitorApplicationService.applyToConvocatoria(any(MonitorApplicationDTO.class)))
                .thenReturn(mockApplication);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/monitor-application/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(monitorApplicationService, times(1)).applyToConvocatoria(any(MonitorApplicationDTO.class));
    }

    /**
     * Test 3: Profesor selecciona monitor
     */
    @Test
    @WithMockUser(roles = "professor")
    public void testSelectMonitor_Success() throws Exception {
        // Arrange
        SelectMonitorRequest request = new SelectMonitorRequest();
        request.setMonitoringRequestId(1L);
        request.setApplicationId(2L);
        request.setProfessorId("1001");
        request.setNotes("Excelente candidato");

        doNothing().when(monitorApplicationService).selectMonitor(any(SelectMonitorRequest.class));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/monitor-application/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(monitorApplicationService, times(1)).selectMonitor(any(SelectMonitorRequest.class));
    }

    /**
     * Test 4: Obtener convocatorias abiertas
     */
    @Test
    @WithMockUser(roles = "student")
    public void testGetOpenConvocatorias_Success() throws Exception {
        // Arrange
        when(monitoringRequestService.findOpenConvocatorias()).thenReturn(Arrays.asList(mockRequest));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/monitoring-request/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(monitoringRequestService, times(1)).findOpenConvocatorias();
    }

    /**
     * Test 5: Obtener postulantes de una convocatoria (RUTA CORREGIDA)
     */
    @Test
    @WithMockUser(roles = "professor")
    public void testGetApplicationsByRequest_Success() throws Exception {
        // Arrange
        Long requestId = 1L;
        when(monitorApplicationService.getApplicationsByRequest(requestId))
                .thenReturn(Arrays.asList(mockApplication));

        // Act & Assert - RUTA CORRECTA: /request/{requestId}
        mockMvc.perform(MockMvcRequestBuilders.get("/monitor-application/request/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(monitorApplicationService, times(1)).getApplicationsByRequest(requestId);
    }

    /**
     * Test 6: Validar justificación requerida al crear convocatoria
     */
    @Test
    @WithMockUser(roles = "professor")
    public void testCreateConvocatoria_FailsWithoutJustification() throws Exception {
        // Arrange
        MonitoringRequestDTO dto = new MonitoringRequestDTO();
        dto.setProfessorId("1001");
        dto.setCourseId(1L);
        dto.setSchoolId(1);
        dto.setProgramId(1L);
        dto.setRequestedHours(20);
        dto.setSemester("2025-1");
        // Sin justificación

        when(monitoringRequestService.createConvocatoria(any(MonitoringRequestDTO.class)))
                .thenThrow(new Exception("La justificación es requerida"));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/monitoring-request/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La justificación es requerida"));

        verify(monitoringRequestService, times(1)).createConvocatoria(any(MonitoringRequestDTO.class));
    }

    /**
     * Test 7: Validar carta de motivación al postularse
     */
    @Test
    @WithMockUser(roles = "student")
    public void testApplyToConvocatoria_FailsWithShortMotivationLetter() throws Exception {
        // Arrange
        MonitorApplicationDTO dto = new MonitorApplicationDTO();
        dto.setMonitoringRequestId(1L);
        dto.setMonitorId("2220001");
        dto.setMotivationLetter("abc"); // Muy corta

        when(monitorApplicationService.applyToConvocatoria(any(MonitorApplicationDTO.class)))
                .thenThrow(new Exception("La carta de motivación debe tener al menos 10 caracteres"));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/monitor-application/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La carta de motivación debe tener al menos 10 caracteres"));

        verify(monitorApplicationService, times(1)).applyToConvocatoria(any(MonitorApplicationDTO.class));
    }

    /**
     * Test 8: Obtener convocatorias del profesor (RUTA CORREGIDA)
     */
    @Test
    @WithMockUser(roles = "professor")
    public void testGetMyRequests_Success() throws Exception {
        // Arrange
        String professorId = "1001";
        when(monitoringRequestService.findByProfessor(professorId))
                .thenReturn(Arrays.asList(mockRequest));
        when(monitoringRequestService.getApplicationCount(anyLong())).thenReturn(0);

        // Act & Assert - RUTA CORRECTA: /professor/{professorId}
        mockMvc.perform(MockMvcRequestBuilders.get("/monitoring-request/professor/{professorId}", professorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(monitoringRequestService, times(1)).findByProfessor(professorId);
    }

    /**
     * Test 9: Obtener mis postulaciones como estudiante (RUTA CORREGIDA)
     */
    @Test
    @WithMockUser(roles = "student")
    public void testGetMyApplications_Success() throws Exception {
        // Arrange
        String monitorId = "2220001";
        when(monitorApplicationService.getApplicationsByMonitor(monitorId))
                .thenReturn(Arrays.asList(mockApplication));

        // Act & Assert - RUTA CORRECTA: /monitor/{monitorId}
        mockMvc.perform(MockMvcRequestBuilders.get("/monitor-application/monitor/{monitorId}", monitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(monitorApplicationService, times(1)).getApplicationsByMonitor(monitorId);
    }

    /**
     * Test 10: Cancelar convocatoria (MÉTODO CORREGIDO: POST en lugar de DELETE)
     */
    @Test
    @WithMockUser(roles = "professor")
    public void testCancelConvocatoria_Success() throws Exception {
        // Arrange
        Long requestId = 1L;
        String professorId = "1001";
        Map<String, String> body = Map.of("professorId", professorId);

        doNothing().when(monitoringRequestService).cancelConvocatoria(requestId, professorId);

        // Act & Assert - MÉTODO CORRECTO: POST
        mockMvc.perform(MockMvcRequestBuilders.post("/monitoring-request/{id}/cancel", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Convocatoria cancelada exitosamente"));

        verify(monitoringRequestService, times(1)).cancelConvocatoria(requestId, professorId);
    }
}

