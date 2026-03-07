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

import com.pdg.sigma.domain.ChatAttachment;
import com.pdg.sigma.domain.ChatMessage;
import com.pdg.sigma.domain.DepartmentHead;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.dto.ChatAttachmentDTO;
import com.pdg.sigma.dto.ChatConversationDTO;
import com.pdg.sigma.dto.ChatMessageCreateDTO;
import com.pdg.sigma.dto.ChatMessageDTO;
import com.pdg.sigma.repository.ChatAttachmentRepository;
import com.pdg.sigma.repository.ChatMessageRepository;
import com.pdg.sigma.repository.DepartmentHeadRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private DepartmentHeadRepository departmentHeadRepository;

    @Autowired
    private DepartmentHeadService departmentHeadService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatAttachmentRepository chatAttachmentRepository;

    @Autowired
    private ChatStorageService chatStorageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initChatSchema() {
        ensureChatTables();
    }

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
                String conversationId = buildProfessorMonitorConversationId(userId, monitorId);
                String monitorName = buildMonitorName(monitor);
                uniqueConversations.putIfAbsent(conversationId,
                        new ChatConversationDTO(conversationId, "Monitor: " + monitorName, "Chat directo", monitorId));
            }

            List<DepartmentHead> heads = departmentHeadRepository.findAll();
            for (DepartmentHead head : heads) {
                if (head.getId() == null || head.getId().isBlank()) {
                    continue;
                }
                String headId = head.getId();
                String conversationId = buildHeadProfessorConversationId(headId, userId);
                uniqueConversations.putIfAbsent(conversationId,
                        new ChatConversationDTO(conversationId,
                                "Jefe de Departamento: " + buildDepartmentHeadName(head),
                                "Chat directo",
                                headId));
            }
        } else if ("monitor".equals(normalizedRole)) {
            List<Professor> professors = professorRepository.findAll();
            for (Professor professor : professors) {
                if (professor.getId() == null || professor.getId().isBlank()) {
                    continue;
                }
                String professorId = professor.getId();
                String professorName = buildProfessorName(professor);
                String conversationId = buildProfessorMonitorConversationId(professorId, userId);
                uniqueConversations.putIfAbsent(conversationId,
                        new ChatConversationDTO(conversationId, "Profesor: " + professorName, "Chat directo", professorId));
            }
        } else if ("jfedpto".equals(normalizedRole)) {
            List<Professor> professors = departmentHeadService.getProfessorsByDepartmentHead(userId);
            for (Professor professor : professors) {
                if (professor.getId() == null || professor.getId().isBlank()) {
                    continue;
                }
                String professorId = professor.getId();
                String professorName = buildProfessorName(professor);
                String conversationId = buildHeadProfessorConversationId(userId, professorId);
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
            // If chat tables are not available yet, keep base ordering.
        }
        return ordered;
    }

    @Override
    public List<ChatMessageDTO> getMessages(String conversationId) {
        try {
            List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            return messages.stream().map(this::toDTO).toList();
        } catch (Exception ignored) {
            // If chat tables are not available yet, return empty list to keep UI usable.
            return List.of();
        }
    }

    @Override
    public ChatMessageDTO sendMessage(ChatMessageCreateDTO payload, List<MultipartFile> files) throws Exception {
        ensureChatTables();
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

    private String buildProfessorMonitorConversationId(String professorId, String monitorId) {
        return "prof-" + professorId + "__mon-" + monitorId;
    }

    private String buildHeadProfessorConversationId(String headId, String professorId) {
        return "head-" + headId + "__prof-" + professorId;
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
            if (isMonitorUser(receiverId)) {
                return buildProfessorMonitorConversationId(payload.getSenderId(), receiverId);
            }
            if (isDepartmentHeadUser(receiverId)) {
                return buildHeadProfessorConversationId(receiverId, payload.getSenderId());
            }
            throw new Exception("receiverId inválido para rol professor");
        }
        if ("monitor".equals(senderRole)) {
            return buildProfessorMonitorConversationId(receiverId, payload.getSenderId());
        }
        if ("jfedpto".equals(senderRole)) {
            if (!isProfessorUser(receiverId)) {
                throw new Exception("receiverId inválido para rol jfedpto");
            }
            return buildHeadProfessorConversationId(payload.getSenderId(), receiverId);
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

        if (conversationId.startsWith(professorPrefix) && sep >= 0) {
            String professorId = conversationId.substring(professorPrefix.length(), sep);
            String monitorId = conversationId.substring(sep + monitorPrefix.length());

            if ("professor".equals(senderRole)) {
                return monitorId;
            }
            if ("monitor".equals(senderRole)) {
                return professorId;
            }
            throw new Exception("Rol no soportado para conversationId professor-monitor");
        }

        String headPrefix = "head-";
        String profSuffix = "__prof-";
        int headSep = conversationId.indexOf(profSuffix);
        if (conversationId.startsWith(headPrefix) && headSep >= 0) {
            String headId = conversationId.substring(headPrefix.length(), headSep);
            String professorId = conversationId.substring(headSep + profSuffix.length());

            if ("jfedpto".equals(senderRole)) {
                return professorId;
            }
            if ("professor".equals(senderRole)) {
                return headId;
            }
            throw new Exception("Rol no soportado para conversationId jefe-profesor");
        }

        throw new Exception("conversationId inválido");
    }

    private boolean isMonitorUser(String userId) {
        return userId != null && monitorRepository.findByIdMonitor(userId).isPresent();
    }

    private boolean isProfessorUser(String userId) {
        return userId != null && professorRepository.existsById(userId);
    }

    private boolean isDepartmentHeadUser(String userId) {
        return userId != null && departmentHeadRepository.existsById(userId);
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

    private String buildDepartmentHeadName(DepartmentHead head) {
        if (head == null) {
            return "Jefe";
        }
        String name = head.getName() != null ? head.getName().trim() : "";
        if (!name.isBlank()) {
            return name;
        }
        return "Jefe " + head.getId();
    }

    private void ensureChatTables() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS sigma");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sigma.chat_message (
                id BIGSERIAL PRIMARY KEY,
                conversation_id VARCHAR(120) NOT NULL,
                sender_id VARCHAR(40) NOT NULL,
                sender_role VARCHAR(20) NOT NULL,
                receiver_id VARCHAR(40) NOT NULL,
                activity_id INTEGER,
                message TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT NOW()
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sigma.chat_attachment (
                id BIGSERIAL PRIMARY KEY,
                message_id BIGINT NOT NULL,
                original_name VARCHAR(255) NOT NULL,
                content_type VARCHAR(120),
                size_bytes BIGINT,
                storage_path VARCHAR(500) NOT NULL,
                CONSTRAINT fk_chat_attachment_message
                    FOREIGN KEY (message_id)
                    REFERENCES sigma.chat_message(id)
                    ON DELETE CASCADE
            )
        """);
    }
}