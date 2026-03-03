package com.pdg.sigma.dto;

import java.util.Date;
import java.util.List;

import com.pdg.sigma.domain.ChatMessage;

import lombok.Data;

@Data
public class ChatMessageDTO {
    private Long id;
    private String conversationId;
    private String senderId;
    private String senderRole;
    private String receiverId;
    private Integer activityId;
    private String message;
    private Date createdAt;
    private List<ChatAttachmentDTO> attachments;

    public ChatMessageDTO(ChatMessage entity, List<ChatAttachmentDTO> attachments) {
        this.id = entity.getId();
        this.conversationId = entity.getConversationId();
        this.senderId = entity.getSenderId();
        this.senderRole = entity.getSenderRole();
        this.receiverId = entity.getReceiverId();
        this.activityId = entity.getActivityId();
        this.message = entity.getMessage();
        this.createdAt = entity.getCreatedAt();
        this.attachments = attachments;
    }
}