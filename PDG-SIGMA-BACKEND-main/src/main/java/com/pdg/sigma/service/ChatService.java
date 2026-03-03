package com.pdg.sigma.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.pdg.sigma.dto.ChatConversationDTO;
import com.pdg.sigma.dto.ChatMessageCreateDTO;
import com.pdg.sigma.dto.ChatMessageDTO;

public interface ChatService {

    List<ChatConversationDTO> getConversations(String userId, String role);

    List<ChatMessageDTO> getMessages(String conversationId);

    ChatMessageDTO sendMessage(ChatMessageCreateDTO payload, List<MultipartFile> files) throws Exception;

    AttachmentDownload downloadAttachment(Long attachmentId) throws Exception;

    record AttachmentDownload(Resource resource, String filename, String contentType) {}
}