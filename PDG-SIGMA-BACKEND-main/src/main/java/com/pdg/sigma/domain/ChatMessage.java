package com.pdg.sigma.domain;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 120)
    private String conversationId;

    @Column(name = "sender_id", nullable = false, length = 40)
    private String senderId;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;

    @Column(name = "receiver_id", nullable = false, length = 40)
    private String receiverId;

    @Column(name = "activity_id")
    private Integer activityId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}