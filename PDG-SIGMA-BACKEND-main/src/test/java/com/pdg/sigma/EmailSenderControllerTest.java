package com.pdg.sigma;

import com.pdg.sigma.controller.EmailSenderController;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.MonitoringMonitor;
import com.pdg.sigma.dto.SelectionResultDTO;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.service.EmailSenderService;
import com.pdg.sigma.service.MonitorServiceImpl;
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

@WebMvcTest(controllers = EmailSenderController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class EmailSenderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailSenderService emailSenderService;

    @MockBean
    private MonitorServiceImpl monitorService;

    @MockBean
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void sendBasicEmail_returnsSuccess() throws Exception {
        doNothing().when(emailSenderService).sendEmail("test@test.com", "Subject", "Body");

        mockMvc.perform(get("/send-basic-email")
                        .param("to", "test@test.com")
                        .param("subject", "Subject")
                        .param("body", "Body"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email sent successfully to test@test.com"));
    }

    @Test
    void sendBasicEmail_throws_returnsErrorMessage() throws Exception {
        doThrow(new RuntimeException("SMTP error")).when(emailSenderService)
                .sendEmail("test@test.com", "Subject", "Body");

        mockMvc.perform(get("/send-basic-email")
                        .param("to", "test@test.com")
                        .param("subject", "Subject")
                        .param("body", "Body"))
                .andExpect(status().isOk())
                .andExpect(content().string("Failed to send email: SMTP error"));
    }

    @Test
    void notifySelectionResults_emptyList_returnsOk() throws Exception {
        mockMvc.perform(post("/email-finish-selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(content().string("Proceso de selección finalizado. Notificaciones enviadas."));

        verify(monitoringMonitorRepository, never()).saveAll(any());
        verify(emailSenderService, never()).sendToMonitors(anyList(), anyBoolean());
    }

    @Test
    void notifySelectionResults_selected_statusChanged_sendsEmail() throws Exception {
        MonitoringMonitor relation = createRelation("M001", "no seleccionado");
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(relation));

        mockMvc.perform(post("/email-finish-selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"code\":\"M001\",\"idMonitoring\":1,\"estadoSeleccion\":\"seleccionado\"}]"))
                .andExpect(status().isOk());

        verify(monitoringMonitorRepository).saveAll(anyList());
        verify(emailSenderService).sendToMonitors(List.of(relation), true);
    }

    @Test
    void notifySelectionResults_notSelected_sendsEmail() throws Exception {
        MonitoringMonitor relation = createRelation("M001", null);
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(relation));

        mockMvc.perform(post("/email-finish-selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"code\":\"M001\",\"idMonitoring\":1,\"estadoSeleccion\":\"no seleccionado\"}]"))
                .andExpect(status().isOk());

        verify(emailSenderService).sendToMonitors(List.of(relation), false);
    }

    @Test
    void notifySelectionResults_alreadySelected_doesNotResendEmail() throws Exception {
        MonitoringMonitor relation = createRelation("M001", "seleccionado");
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(relation));

        mockMvc.perform(post("/email-finish-selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"code\":\"M001\",\"idMonitoring\":1,\"estadoSeleccion\":\"seleccionado\"}]"))
                .andExpect(status().isOk());

        verify(monitoringMonitorRepository).saveAll(anyList());
        verify(emailSenderService, never()).sendToMonitors(anyList(), anyBoolean());
    }

    @Test
    void notifySelectionResults_unknownStatus_ignored() throws Exception {
        MonitoringMonitor relation = createRelation("M001", null);
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(relation));

        mockMvc.perform(post("/email-finish-selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"code\":\"M001\",\"idMonitoring\":1,\"estadoSeleccion\":\"unknown\"}]"))
                .andExpect(status().isOk());

        verify(monitoringMonitorRepository).saveAll(anyList());
        verify(emailSenderService, never()).sendToMonitors(anyList(), anyBoolean());
    }

    @Test
    void notifySelectionResults_relationNotFound_skips() throws Exception {
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/email-finish-selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"code\":\"M001\",\"idMonitoring\":1,\"estadoSeleccion\":\"seleccionado\"}]"))
                .andExpect(status().isOk());

        verify(monitoringMonitorRepository, never()).saveAll(any());
        verify(emailSenderService, never()).sendToMonitors(anyList(), anyBoolean());
    }

    @Test
    void notifySelectionResults_mixedResults_sendsBoth() throws Exception {
        MonitoringMonitor selectedRel = createRelation("M001", null);
        MonitoringMonitor notSelectedRel = createRelation("M002", "pendiente");

        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(1L, "M001"))
                .thenReturn(Optional.of(selectedRel));
        when(monitoringMonitorRepository.findByMonitoringIdAndMonitorCode(2L, "M002"))
                .thenReturn(Optional.of(notSelectedRel));

        mockMvc.perform(post("/email-finish-selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" +
                                "{\"code\":\"M001\",\"idMonitoring\":1,\"estadoSeleccion\":\"seleccionado\"}," +
                                "{\"code\":\"M002\",\"idMonitoring\":2,\"estadoSeleccion\":\"no seleccionado\"}" +
                                "]"))
                .andExpect(status().isOk());

        verify(monitoringMonitorRepository).saveAll(anyList());
        verify(emailSenderService).sendToMonitors(List.of(selectedRel), true);
        verify(emailSenderService).sendToMonitors(List.of(notSelectedRel), false);
    }

    private MonitoringMonitor createRelation(String code, String estadoSeleccion) {
        Monitor monitor = new Monitor();
        monitor.setCode(code);
        monitor.setIdMonitor(code);
        monitor.setName("Monitor " + code);

        MonitoringMonitor relation = new MonitoringMonitor();
        relation.setMonitor(monitor);
        relation.setMonitoring(new Monitoring());
        relation.setEstadoSeleccion(estadoSeleccion);
        return relation;
    }
}
