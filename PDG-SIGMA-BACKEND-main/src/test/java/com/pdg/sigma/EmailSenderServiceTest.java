package com.pdg.sigma;

import com.pdg.sigma.domain.Course;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.MonitoringMonitor;
import com.pdg.sigma.service.EmailSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailSenderService emailSenderService;

    private MonitoringMonitor selectedRelation;
    private MonitoringMonitor rejectedRelation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Monitor monitor = new Monitor();
        monitor.setCode("M001");
        monitor.setName("Carlos");
        monitor.setEmail("carlos@test.com");

        Course course = new Course();
        course.setName("POO");

        Monitoring monitoring = new Monitoring();
        monitoring.setId(1L);
        monitoring.setCourse(course);

        selectedRelation = new MonitoringMonitor(monitoring, monitor, "seleccionado");

        rejectedRelation = new MonitoringMonitor(monitoring, monitor, "no seleccionado");
    }

    @Test
    @DisplayName("Debe enviar email simple")
    void testSendEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailSenderService.sendEmail("test@test.com", "Asunto", "Cuerpo");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Debe enviar email a monitor seleccionado")
    void testSendToMonitorsSelected() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailSenderService.sendToMonitors(List.of(selectedRelation), false);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Debe enviar email a monitor no seleccionado")
    void testSendToMonitorsRejected() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailSenderService.sendToMonitors(List.of(rejectedRelation), false);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("No debe enviar email si la lista está vacía")
    void testSendToMonitorsEmptyList() {
        emailSenderService.sendToMonitors(List.of(), false);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("No debe enviar email si la lista es nula")
    void testSendToMonitorsNullList() {
        emailSenderService.sendToMonitors(null, false);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Debe saltar monitor sin email")
    void testSendToMonitorsNoEmail() {
        Monitor noEmailMonitor = new Monitor();
        noEmailMonitor.setCode("M002");
        noEmailMonitor.setName("Test");

        MonitoringMonitor relation = new MonitoringMonitor();
        relation.setMonitor(noEmailMonitor);

        emailSenderService.sendToMonitors(List.of(relation), false);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
