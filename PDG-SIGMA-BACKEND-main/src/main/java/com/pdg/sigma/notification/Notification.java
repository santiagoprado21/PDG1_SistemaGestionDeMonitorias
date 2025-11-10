package com.pdg.sigma.notification;

import java.util.Date;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "notification")
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professor_id", nullable = false)
    private String professorId; // destinatario

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "activity_id")
    private Integer activityId; // link directo

    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag = false;

    public Notification(String professorId, NotificationType type, String message, Integer activityId) {
        this.professorId = professorId;
        this.type = type;
        this.message = message;
        this.activityId = activityId;
    }
}
