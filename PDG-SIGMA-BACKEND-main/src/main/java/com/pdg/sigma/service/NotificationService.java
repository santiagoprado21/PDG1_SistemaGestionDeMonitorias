package com.pdg.sigma.service;

import java.util.List;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.dto.NotificationDTO;

public interface NotificationService {
    void notifyProgressUpdate(Activity activity);
    void notifyCompleted(Activity activity);
    void notifyOverdue(Activity activity);

    List<NotificationDTO> getUnreadForProfessor(String professorId);
    long getUnreadCount(String professorId);
    void markAsRead(Long id);
    void markAllAsRead(String professorId);
}
