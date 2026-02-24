package com.pdg.sigma.dto;

import lombok.Data;

@Data
public class ChatMessageCreateDTO {
    private String conversationId;
    private String senderId;
    private String senderRole;
    private String receiverId;
    private Integer activityId;
    private String message;
}