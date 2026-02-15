package com.pdg.sigma;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.StateActivity;
import com.pdg.sigma.notification.NotificationPreference;
import com.pdg.sigma.repository.NotificationPreferenceRepository;
import com.pdg.sigma.repository.NotificationRepository;
import com.pdg.sigma.service.NotificationPreferenceServiceImpl;
import com.pdg.sigma.service.NotificationServiceImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({NotificationServiceImpl.class, NotificationPreferenceServiceImpl.class})
class NotificationServiceImplTest {

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Activity baseActivity;

    @BeforeEach
    void setup() {
        Professor prof = new Professor();
        prof.setId("PROF1");
        prof.setName("Juan");
        prof.setPassword("x");
        Monitor monitor = new Monitor();
        monitor.setCode("MON1");
        monitor.setName("Ana");
        monitor.setLastName("Gomez");
        monitor.setSemester(5);
        monitor.setEmail("ana@example.com");
        monitor.setIdMonitor("123");

        baseActivity = new Activity();
        baseActivity.setId(1); // no importa persistencia completa para notificaciones
        baseActivity.setName("Preparar informe");
        baseActivity.setCreation(new Date());
        baseActivity.setFinish(new Date(System.currentTimeMillis() + 86400000));
        baseActivity.setRoleCreator("P");
        baseActivity.setRoleResponsable("M");
        baseActivity.setCategory("DOC");
        baseActivity.setDescription("Informe mensual");
        baseActivity.setProfessor(prof);
        baseActivity.setMonitor(monitor);
        baseActivity.setState(StateActivity.PENDIENTE);
        baseActivity.setSemester("2025-2");
        baseActivity.setDelivey(new Date());
        baseActivity.setEdited(new Date());

        // preferencias por defecto (todas activas)
        preferenceRepository.save(new NotificationPreference("PROF1"));
    }

    @Test
    void createsProgressNotificationWhenEnabled() {
        notificationService.notifyProgressUpdate(baseActivity);
        assertThat(notificationRepository.findAll()).hasSize(1);
        assertThat(notificationRepository.findAll().get(0).getType().name()).isEqualTo("PROGRESS_UPDATE");
    }

    @Test
    void respectsDisabledCompletedPreference() {
        NotificationPreference pref = preferenceRepository.findByProfessorId("PROF1").get();
        pref.setEnableCompleted(false);
        preferenceRepository.save(pref);
        notificationService.notifyCompleted(baseActivity);
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    void overdueNotificationDeduplicates() {
        notificationService.notifyOverdue(baseActivity);
        notificationService.notifyOverdue(baseActivity); // segundo intento no debería duplicar
        assertThat(notificationRepository.findAll()).hasSize(1);
        assertThat(notificationRepository.findAll().get(0).getType().name()).isEqualTo("OVERDUE");
    }

    @Test
    void markAllAsReadClearsUnreadCount() {
        notificationService.notifyProgressUpdate(baseActivity);
        notificationService.notifyCompleted(baseActivity);
        assertThat(notificationService.getUnreadCount("PROF1")).isEqualTo(2);
        notificationService.markAllAsRead("PROF1");
        assertThat(notificationService.getUnreadCount("PROF1")).isZero();
    }
}
