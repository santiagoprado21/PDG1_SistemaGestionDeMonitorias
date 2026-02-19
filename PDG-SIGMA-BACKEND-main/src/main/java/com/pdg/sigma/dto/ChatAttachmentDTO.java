package com.pdg.sigma.dto;

import com.pdg.sigma.domain.ChatAttachment;

import lombok.Data;

@Data
public class ChatAttachmentDTO {
    private Long id;
    private String name;
    private String contentType;
    private Long size;
    private String url;

    public ChatAttachmentDTO(ChatAttachment attachment, String url) {
        this.id = attachment.getId();
        this.name = attachment.getOriginalName();
        this.contentType = attachment.getContentType();
        this.size = attachment.getSizeBytes();
        this.url = url;
    }
}
