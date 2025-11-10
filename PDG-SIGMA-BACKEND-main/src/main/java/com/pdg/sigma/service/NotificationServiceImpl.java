package com.pdg.sigma.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.dto.NotificationDTO;
import com.pdg.sigma.notification.Notification;
import com.pdg.sigma.notification.NotificationType;
import com.pdg.sigma.repository.NotificationRepository;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${sigma.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${sigma.notifications.realtime-enabled:true}")
    private boolean realtimeEnabled;

    @Autowired
    private NotificationPreferenceServiceImpl preferenceService;

    // Simple in-memory broadcaster per professor for SSE
    private final java.util.concurrent.ConcurrentHashMap<String, Sinks.Many<NotificationDTO>> sinks = new java.util.concurrent.ConcurrentHashMap<>();

    private boolean shouldNotifyProfessor(Activity activity) {
        return notificationsEnabled && activity.getProfessor() != null;
    }

    public Flux<ServerSentEvent<NotificationDTO>> subscribe(String professorId) {
        Sinks.Many<NotificationDTO> sink = sinks.computeIfAbsent(professorId, k -> Sinks.many().multicast().onBackpressureBuffer());
        return sink.asFlux().map(data -> ServerSentEvent.builder(data).build());
    }

    @Override
    public void notifyProgressUpdate(Activity activity) {
        if (!shouldNotifyProfessor(activity)) return;
        // Respect user preference
        if (!preferenceService.isTypeEnabled(activity.getProfessor().getId(), NotificationType.PROGRESS_UPDATE)) return;
        String msg = String.format("El monitor %s actualizó la actividad '%s'.", 
            activity.getMonitor() != null ? (activity.getMonitor().getName() + " " + activity.getMonitor().getLastName()) : "",
            activity.getName());
        Notification n = new Notification(activity.getProfessor().getId(), NotificationType.PROGRESS_UPDATE, msg, activity.getId());
        n.setCreatedAt(new Date());
    // flush immediately so integration tests / subsequent HTTP requests can see it
    n = notificationRepository.saveAndFlush(n);
        if (realtimeEnabled) {
            Sinks.Many<NotificationDTO> sink = sinks.get(activity.getProfessor().getId());
            if (sink != null) sink.tryEmitNext(new NotificationDTO(n));
        }
    }

    @Override
    public void notifyCompleted(Activity activity) {
        if (!shouldNotifyProfessor(activity)) return;
        if (!preferenceService.isTypeEnabled(activity.getProfessor().getId(), NotificationType.COMPLETED)) return;
        String msg = String.format("La actividad '%s' fue marcada como completada.", activity.getName());
        Notification n = new Notification(activity.getProfessor().getId(), NotificationType.COMPLETED, msg, activity.getId());
        n.setCreatedAt(new Date());
    n = notificationRepository.saveAndFlush(n);
        if (realtimeEnabled) {
            Sinks.Many<NotificationDTO> sink = sinks.get(activity.getProfessor().getId());
            if (sink != null) sink.tryEmitNext(new NotificationDTO(n));
        }
    }

    @Override
    public void notifyOverdue(Activity activity) {
        if (!shouldNotifyProfessor(activity)) return;
        // Evitar duplicados no leídos para la misma actividad
        if (notificationRepository.existsByProfessorIdAndActivityIdAndReadFlagFalse(activity.getProfessor().getId(), activity.getId())) {
            return;
        }
        if (!preferenceService.isTypeEnabled(activity.getProfessor().getId(), NotificationType.OVERDUE)) return;
        String msg = String.format("La actividad '%s' está atrasada.", activity.getName());
        Notification n = new Notification(activity.getProfessor().getId(), NotificationType.OVERDUE, msg, activity.getId());
        n.setCreatedAt(new Date());
    n = notificationRepository.saveAndFlush(n);
        if (realtimeEnabled) {
            Sinks.Many<NotificationDTO> sink = sinks.get(activity.getProfessor().getId());
            if (sink != null) sink.tryEmitNext(new NotificationDTO(n));
        }
    }

    @Override
    public List<NotificationDTO> getUnreadForProfessor(String professorId) {
        return notificationRepository.findByProfessorIdAndReadFlagFalseOrderByCreatedAtDesc(professorId)
            .stream().map(NotificationDTO::new).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(String professorId) {
        return notificationRepository.findByProfessorIdAndReadFlagFalseOrderByCreatedAtDesc(professorId).size();
    }

    @Override
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> { n.setReadFlag(true); notificationRepository.save(n); });
    }

    @Override
    public void markAllAsRead(String professorId) {
        List<Notification> list = notificationRepository.findByProfessorIdAndReadFlagFalseOrderByCreatedAtDesc(professorId);
        list.forEach(n -> n.setReadFlag(true));
        notificationRepository.saveAll(list);
    }
}
