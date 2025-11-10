package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.controller.NotificationController;
import com.pdg.sigma.dto.NotificationDTO;
import com.pdg.sigma.dto.NotificationPreferenceDTO;
import com.pdg.sigma.notification.Notification;
import com.pdg.sigma.notification.NotificationType;
import com.pdg.sigma.service.NotificationPreferenceService;
import com.pdg.sigma.service.NotificationService;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationPreferenceService preferenceService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
        void getUnread_returnsList() throws Exception {
                String professorId = "prof-123";
                Notification n = new Notification(professorId, NotificationType.PROGRESS_UPDATE, "New activity updated", 42);
                n.setId(1L);
                NotificationDTO dto = new NotificationDTO(n);
                Mockito.when(notificationService.getUnreadForProfessor(professorId))
                                .thenReturn(List.of(dto));

        mockMvc.perform(get("/notifications/unread/{professorId}", professorId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].professorId", is(professorId)))
                .andExpect(jsonPath("$[0].type", is("PROGRESS_UPDATE")))
                .andExpect(jsonPath("$[0].read", is(false)));
    }

    @Test
    void getCount_returnsNumber() throws Exception {
        String professorId = "prof-123";
        Mockito.when(notificationService.getUnreadCount(professorId)).thenReturn(3L);

        mockMvc.perform(get("/notifications/count/{professorId}", professorId))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void markOne_marksAsRead() throws Exception {
        Long id = 99L;
        mockMvc.perform(put("/notifications/{id}/read", id))
                .andExpect(status().isOk());
        Mockito.verify(notificationService).markAsRead(id);
    }

    @Test
    void markAll_marksAllAsRead() throws Exception {
        String professorId = "prof-xyz";
        mockMvc.perform(put("/notifications/read-all/{professorId}", professorId))
                .andExpect(status().isOk());
        Mockito.verify(notificationService).markAllAsRead(professorId);
    }

    @Test
    void getPrefs_returnsDto() throws Exception {
        String professorId = "prof-abc";
        NotificationPreferenceDTO prefs = new NotificationPreferenceDTO();
        prefs.setProfessorId(professorId);
        prefs.setEnableProgressUpdate(true);
        prefs.setEnableCompleted(false);
        prefs.setEnableOverdue(true);
        prefs.setEnableSound(true);
        Mockito.when(preferenceService.getPreferences(professorId)).thenReturn(prefs);

        mockMvc.perform(get("/notifications/prefs/{professorId}", professorId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.professorId", is(professorId)))
                .andExpect(jsonPath("$.enableProgressUpdate", is(true)))
                .andExpect(jsonPath("$.enableCompleted", is(false)))
                .andExpect(jsonPath("$.enableOverdue", is(true)))
                .andExpect(jsonPath("$.enableSound", is(true)));
    }

    @Test
    void updatePrefs_updatesAndReturnsDto() throws Exception {
        String professorId = "prof-abc";
        NotificationPreferenceDTO input = new NotificationPreferenceDTO();
        input.setEnableProgressUpdate(false);
        input.setEnableCompleted(true);
        input.setEnableOverdue(false);
        input.setEnableSound(false);

        NotificationPreferenceDTO saved = new NotificationPreferenceDTO();
        saved.setProfessorId(professorId);
        saved.setEnableProgressUpdate(false);
        saved.setEnableCompleted(true);
        saved.setEnableOverdue(false);
        saved.setEnableSound(false);

        Mockito.when(preferenceService.updatePreferences(any(NotificationPreferenceDTO.class)))
                .thenReturn(saved);

        mockMvc.perform(put("/notifications/prefs/{professorId}", professorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professorId", is(professorId)))
                .andExpect(jsonPath("$.enableCompleted", is(true)))
                .andExpect(jsonPath("$.enableProgressUpdate", is(false)))
                .andExpect(jsonPath("$.enableOverdue", is(false)))
                .andExpect(jsonPath("$.enableSound", is(false)));

        Mockito.verify(preferenceService).updatePreferences(any(NotificationPreferenceDTO.class));
    }
}
