package com.pdg.sigma.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pdg.sigma.dto.ChatConversationDTO;
import com.pdg.sigma.dto.ChatMessageCreateDTO;
import com.pdg.sigma.dto.ChatMessageDTO;
import com.pdg.sigma.service.ChatService;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RequestMapping("/chat")
@RestController
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/conversations/{userId}/{role}")
    public ResponseEntity<?> getConversations(@PathVariable String userId, @PathVariable String role) {
        try {
            List<ChatConversationDTO> conversations = chatService.getConversations(userId, role);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<?> getMessages(@PathVariable String conversationId) {
        try {
            List<ChatMessageDTO> messages = chatService.getMessages(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendMessage(@org.springframework.web.bind.annotation.RequestBody ChatMessageCreateDTO payload) {
        try {
            ChatMessageDTO created = chatService.sendMessage(payload, List.of());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/messages/with-attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessageWithAttachments(
            @RequestPart("payload") ChatMessageCreateDTO payload,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        try {
            ChatMessageDTO created = chatService.sendMessage(payload, files);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<?> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            ChatService.AttachmentDownload download = chatService.downloadAttachment(attachmentId);
            String contentDisposition = "attachment; filename=\"" +
                    URLEncoder.encode(download.filename(), StandardCharsets.UTF_8) + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(download.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(download.resource());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
