package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatConversationDTO {
    private String id;
    private String title;
    private String subtitle;
    private String otherUserId;
}