package com.pdg.sigma.dto;

import java.util.Date;

import com.pdg.sigma.notification.Notification;
import com.pdg.sigma.notification.NotificationType;

import lombok.Data;

@Data
public class NotificationDTO {
    private Long id;
    private String professorId;
    private NotificationType type;
    private String message;
    private Integer activityId;
    private Date createdAt;
    private boolean read;

    public NotificationDTO(Notification n) {
        this.id = n.getId();
        this.professorId = n.getProfessorId();
        this.type = n.getType();
        this.message = n.getMessage();
        this.activityId = n.getActivityId();
        this.createdAt = n.getCreatedAt();
        this.read = n.isReadFlag();
    }
}
