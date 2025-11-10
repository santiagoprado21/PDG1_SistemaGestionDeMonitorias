package com.pdg.sigma.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

import com.pdg.sigma.dto.NotificationDTO;
import com.pdg.sigma.dto.NotificationPreferenceDTO;
import com.pdg.sigma.service.NotificationService;
import com.pdg.sigma.service.NotificationPreferenceService;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RequestMapping("/notifications")
@RestController
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationPreferenceService preferenceService;

    @GetMapping("/unread/{professorId}")
    public ResponseEntity<List<NotificationDTO>> getUnread(@PathVariable String professorId) {
        return ResponseEntity.ok(notificationService.getUnreadForProfessor(professorId));
    }

    @GetMapping("/count/{professorId}")
    public ResponseEntity<Long> getCount(@PathVariable String professorId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(professorId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markOne(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all/{professorId}")
    public ResponseEntity<Void> markAll(@PathVariable String professorId) {
        notificationService.markAllAsRead(professorId);
        return ResponseEntity.ok().build();
    }

    // Preferences endpoints
    @GetMapping("/prefs/{professorId}")
    public ResponseEntity<NotificationPreferenceDTO> getPrefs(@PathVariable String professorId) {
        return ResponseEntity.ok(preferenceService.getPreferences(professorId));
    }

    @PutMapping("/prefs/{professorId}")
    public ResponseEntity<NotificationPreferenceDTO> updatePrefs(@PathVariable String professorId, @RequestBody NotificationPreferenceDTO dto) {
        dto.setProfessorId(professorId);
        return ResponseEntity.ok(preferenceService.updatePreferences(dto));
    }

    // Real-time SSE stream
    @GetMapping(path = "/stream/{professorId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationDTO>> stream(@PathVariable String professorId) {
        if (notificationService instanceof com.pdg.sigma.service.NotificationServiceImpl impl) {
            return impl.subscribe(professorId);
        }
        return Flux.never();
    }
}
