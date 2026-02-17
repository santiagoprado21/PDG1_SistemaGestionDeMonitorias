package com.pdg.sigma.service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pdg.sigma.domain.ChatAttachment;
import com.pdg.sigma.domain.ChatMessage;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.MonitoringMonitor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.dto.ChatAttachmentDTO;
import com.pdg.sigma.dto.ChatConversationDTO;
import com.pdg.sigma.dto.ChatMessageCreateDTO;
import com.pdg.sigma.dto.ChatMessageDTO;
import com.pdg.sigma.repository.ChatAttachmentRepository;
import com.pdg.sigma.repository.ChatMessageRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.ProfessorRepository;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatAttachmentRepository chatAttachmentRepository;

    @Autowired
    private ChatStorageService chatStorageService;

    @Override
    public List<ChatConversationDTO> getConversations(String userId, String role) {
        String normalizedRole = normalizeRole(role);
        Map<String, ChatConversationDTO> uniqueConversations = new LinkedHashMap<>();

        if ("professor".equals(normalizedRole)) {
            List<Monitor> monitors = monitorRepository.findAll();
            for (Monitor monitor : monitors) {
                if (monitor.getIdMonitor() == null || monitor.getIdMonitor().isBlank()) {
                    continue;
                }
                String monitorId = monitor.getIdMonitor();
                String conversationId = buildConversationId(userId, monitorId);
                String monitorName = buildMonitorName(monitor);
                uniqueConversations.putIfAbsent(conversationId,
                        new ChatConversationDTO(conversationId, "Monitor: " + monitorName, "Chat directo", monitorId));
            }
        } else if ("monitor".equals(normalizedRole)) {
            List<Professor> professors = professorRepository.findAll();
            for (Professor professor : professors) {
                if (professor.getId() == null || professor.getId().isBlank()) {
                    continue;
                }
                String professorId = professor.getId();
                String professorName = buildProfessorName(professor);
                String conversationId = buildConversationId(professorId, userId);
                uniqueConversations.putIfAbsent(conversationId,
                        new ChatConversationDTO(conversationId, "Profesor: " + professorName, "Chat directo", professorId));
            }
        } else {
            return List.of();
        }

        List<ChatConversationDTO> ordered = new ArrayList<>(uniqueConversations.values());
        try {
            ordered.sort(Comparator.comparing((ChatConversationDTO c) ->
                    chatMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(c.getId())
                            .map(ChatMessage::getCreatedAt)
                            .orElse(new java.util.Date(0L))).reversed());
        } catch (Exception ignored) {
            // Si la tabla chat_message no existe aún, mantenemos el orden actual para no bloquear el listado.
        }
        return ordered;
    }

    @Override
    public List<ChatMessageDTO> getMessages(String conversationId) {
        try {
            List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            return messages.stream().map(this::toDTO).toList();
        } catch (Exception ignored) {
            // Si la tabla chat_message no existe aún, devolvemos historial vacío para mantener la UI operativa.
            return List.of();
        }
    }

    @Override
    public ChatMessageDTO sendMessage(ChatMessageCreateDTO payload, List<MultipartFile> files) throws Exception {
        validatePayload(payload, files);

        String senderRole = normalizeRole(payload.getSenderRole());
        String conversationId = resolveConversationId(payload, senderRole);
        String receiverId = resolveReceiverId(payload, senderRole, conversationId);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(payload.getSenderId());
        message.setSenderRole(senderRole);
        message.setReceiverId(receiverId);
        message.setActivityId(payload.getActivityId());
        message.setMessage(payload.getMessage() != null ? payload.getMessage().trim() : null);

        ChatMessage saved = chatMessageRepository.save(message);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                ChatStorageService.StoredChatAttachment stored = chatStorageService.store(saved.getId(), file);
                ChatAttachment attachment = new ChatAttachment();
                attachment.setMessage(saved);
                attachment.setOriginalName(stored.originalFilename());
                attachment.setContentType(file.getContentType());
                attachment.setSizeBytes(file.getSize());
                attachment.setStoragePath(stored.relativePath());
                chatAttachmentRepository.save(attachment);
            }
        }

        return toDTO(saved);
    }

    @Override
    public AttachmentDownload downloadAttachment(Long attachmentId) throws Exception {
        ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new Exception("Adjunto no encontrado"));

        var resource = chatStorageService.loadAsResource(attachment.getStoragePath());
        String contentType = attachment.getContentType();
        if (contentType == null || contentType.isBlank()) {
            try {
                contentType = Files.probeContentType(resource.getFile().toPath());
            } catch (IOException ignored) {
            }
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new AttachmentDownload(resource, attachment.getOriginalName(), contentType);
    }

    private ChatMessageDTO toDTO(ChatMessage message) {
        List<ChatAttachment> attachments = chatAttachmentRepository.findByMessageIdOrderByIdAsc(message.getId());
        List<ChatAttachmentDTO> attachmentDTOs = attachments.stream()
                .map(att -> new ChatAttachmentDTO(att, "/chat/attachments/" + att.getId()))
                .toList();
        return new ChatMessageDTO(message, attachmentDTOs);
    }

    private void validatePayload(ChatMessageCreateDTO payload, List<MultipartFile> files) throws Exception {
        if (payload == null) {
            throw new Exception("Payload de mensaje requerido");
        }
        if (payload.getSenderId() == null || payload.getSenderId().isBlank()) {
            throw new Exception("senderId es requerido");
        }
        if (payload.getSenderRole() == null || payload.getSenderRole().isBlank()) {
            throw new Exception("senderRole es requerido");
        }

        boolean hasText = payload.getMessage() != null && !payload.getMessage().trim().isBlank();
        boolean hasFiles = files != null && files.stream().anyMatch(f -> f != null && !f.isEmpty());
        if (!hasText && !hasFiles) {
            throw new Exception("El mensaje debe incluir texto o al menos un archivo adjunto");
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private String buildConversationId(String professorId, String monitorId) {
        return "prof-" + professorId + "__mon-" + monitorId;
    }

    private String resolveConversationId(ChatMessageCreateDTO payload, String senderRole) throws Exception {
        if (payload.getConversationId() != null && !payload.getConversationId().isBlank()) {
            return payload.getConversationId().trim();
        }

        String receiverId = payload.getReceiverId();
        if (receiverId == null || receiverId.isBlank()) {
            throw new Exception("receiverId es requerido cuando conversationId no viene informado");
        }

        if ("professor".equals(senderRole)) {
            return buildConversationId(payload.getSenderId(), receiverId);
        }
        if ("monitor".equals(senderRole)) {
            return buildConversationId(receiverId, payload.getSenderId());
        }
        throw new Exception("Rol no soportado para chat: " + senderRole);
    }

    private String resolveReceiverId(ChatMessageCreateDTO payload, String senderRole, String conversationId) throws Exception {
        if (payload.getReceiverId() != null && !payload.getReceiverId().isBlank()) {
            return payload.getReceiverId().trim();
        }

        String professorPrefix = "prof-";
        String monitorPrefix = "__mon-";
        int sep = conversationId.indexOf(monitorPrefix);
        if (!conversationId.startsWith(professorPrefix) || sep < 0) {
            throw new Exception("conversationId inválido");
        }
        String professorId = conversationId.substring(professorPrefix.length(), sep);
        String monitorId = conversationId.substring(sep + monitorPrefix.length());

        if ("professor".equals(senderRole)) {
            return monitorId;
        }
        if ("monitor".equals(senderRole)) {
            return professorId;
        }
        throw new Exception("Rol no soportado para chat: " + senderRole);
    }

    private String buildMonitorName(Monitor monitor) {
        if (monitor == null) {
            return "Monitor";
        }
        String full = ((monitor.getName() != null ? monitor.getName() : "") + " " +
                (monitor.getLastName() != null ? monitor.getLastName() : "")).trim();
        if (!full.isBlank()) {
            return full;
        }

        Optional<Monitor> fromRepo = monitorRepository.findById(monitor.getCode());
        return fromRepo.map(m -> ((m.getName() != null ? m.getName() : "") + " " +
                (m.getLastName() != null ? m.getLastName() : "")).trim())
                .filter(s -> !s.isBlank())
                .orElse("Monitor " + monitor.getCode());
    }

    private String buildProfessorName(Professor professor) {
        if (professor == null) {
            return "Profesor";
        }
        String full = professor.getName() != null ? professor.getName().trim() : "";
        if (!full.isBlank()) {
            return full;
        }

        Optional<Professor> fromRepo = professorRepository.findById(professor.getId());
        return fromRepo.map(p -> p.getName() != null ? p.getName().trim() : "")
                .filter(s -> !s.isBlank())
                .orElse("Profesor " + professor.getId());
    }
}
