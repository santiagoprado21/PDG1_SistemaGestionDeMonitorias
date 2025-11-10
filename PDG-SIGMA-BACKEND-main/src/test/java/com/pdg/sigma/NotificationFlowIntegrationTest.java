package com.pdg.sigma;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.StateActivity;
import com.pdg.sigma.notification.NotificationPreference;
import com.pdg.sigma.repository.NotificationPreferenceRepository;
import com.pdg.sigma.service.NotificationServiceImpl;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class NotificationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    // Security filters are disabled via @AutoConfigureMockMvc(addFilters = false)

    @MockBean
    private JavaMailSender javaMailSender; // mock mail sender required by EmailSenderService

    private Activity activity;
    private static final String PROF = "PROF_INT";

    @BeforeEach
    void init() {
        // ensure default preferences exist
        if (preferenceRepository.findByProfessorId(PROF).isEmpty()) {
            preferenceRepository.save(new NotificationPreference(PROF));
        }

        Professor prof = new Professor();
        prof.setId(PROF);
        prof.setName("Profe");
        prof.setPassword("x");

        Monitor monitor = new Monitor();
        monitor.setCode("MON_INT");
        monitor.setName("Ana");
        monitor.setLastName("Gomez");
        monitor.setSemester(6);
        monitor.setEmail("ana@example.com");
        monitor.setIdMonitor("999");

        activity = new Activity();
        activity.setId(101);
        activity.setName("Entrega informe");
        activity.setCreation(new Date());
        activity.setFinish(new Date(System.currentTimeMillis() + 3_600_000));
        activity.setRoleCreator("P");
        activity.setRoleResponsable("M");
        activity.setCategory("DOC");
        activity.setDescription("Informe mensual");
        activity.setProfessor(prof);
        activity.setMonitor(monitor);
        activity.setState(StateActivity.PENDIENTE);
        activity.setSemester("2025-2");
        activity.setDelivey(new Date());
        activity.setEdited(new Date());
    }

    @Test
    void endToEndFlow_create_list_count_markAll_read() throws Exception {
        // Create two notifications
        notificationService.notifyProgressUpdate(activity);
        notificationService.notifyCompleted(activity);

        // count should be 2
        mockMvc.perform(get("/notifications/count/" + PROF))
            .andExpect(status().isOk())
            .andExpect(content().string("2"));

        // list unread should have 2 items
        mockMvc.perform(get("/notifications/unread/" + PROF)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(2)));

        // mark all as read
        mockMvc.perform(put("/notifications/read-all/" + PROF))
            .andExpect(status().isOk());

        // count should be 0 now
        mockMvc.perform(get("/notifications/count/" + PROF))
            .andExpect(status().isOk())
            .andExpect(content().string("0"));

        // disabling PROGRESS should block new progress notifications
        var pref = preferenceRepository.findByProfessorId(PROF).orElseThrow();
        pref.setEnableProgressUpdate(false);
        preferenceRepository.save(pref);
        notificationService.notifyProgressUpdate(activity);

        mockMvc.perform(get("/notifications/count/" + PROF))
            .andExpect(status().isOk())
            .andExpect(content().string("0"));
    }
}
