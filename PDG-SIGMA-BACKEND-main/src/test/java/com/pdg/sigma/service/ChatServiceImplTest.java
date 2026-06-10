package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private DepartmentHeadRepository departmentHeadRepository;

    @Mock
    private DepartmentHeadService departmentHeadService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatAttachmentRepository chatAttachmentRepository;

    @Mock
    private ChatStorageService chatStorageService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private Monitor sampleMonitor;
    private Professor sampleProfessor;
    private DepartmentHead sampleHead;

    @BeforeEach
    void setUp() {
        sampleMonitor = new Monitor();
        sampleMonitor.setCode("M001");
        sampleMonitor.setIdMonitor("M001");
        sampleMonitor.setName("Carlos");
        sampleMonitor.setLastName("Pérez");

        sampleProfessor = new Professor();
        sampleProfessor.setId("P001");
        sampleProfessor.setName("Dr. Juan");

        sampleHead = new DepartmentHead();
        sampleHead.setId("H001");
        sampleHead.setName("Jefe María");
    }

    // ---- GET CONVERSATIONS ----

    @Test
    void getConversations_asProfessor_returnsList() {
        when(monitorRepository.findAll()).thenReturn(List.of(sampleMonitor));
        when(departmentHeadRepository.findAll()).thenReturn(List.of(sampleHead));

        List<ChatConversationDTO> result = chatService.getConversations("P001", "professor");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getId().contains("prof-P001__mon-M001")));
        assertTrue(result.stream().anyMatch(c -> c.getId().contains("head-H001__prof-P001")));
    }

    @Test
    void getConversations_asProfessor_skipsMonitorWithoutId() {
        Monitor noIdMonitor = new Monitor();
        noIdMonitor.setCode("M002");
        noIdMonitor.setIdMonitor(null);
        when(monitorRepository.findAll()).thenReturn(List.of(noIdMonitor));
        when(departmentHeadRepository.findAll()).thenReturn(List.of(sampleHead));

        List<ChatConversationDTO> result = chatService.getConversations("P001", "professor");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getId().contains("head-H001__prof-P001")));
    }

    @Test
    void getConversations_asMonitor_returnsList() {
        when(professorRepository.findAll()).thenReturn(List.of(sampleProfessor));

        List<ChatConversationDTO> result = chatService.getConversations("M001", "monitor");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getId().contains("prof-P001__mon-M001")));
    }

    @Test
    void getConversations_asDepartmentHead_returnsList() {
        when(departmentHeadService.getProfessorsByDepartmentHead("H001")).thenReturn(List.of(sampleProfessor));

        List<ChatConversationDTO> result = chatService.getConversations("H001", "jfedpto");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getId().contains("head-H001__prof-P001")));
    }

    @Test
    void getConversations_unknownRole_returnsEmpty() {
        List<ChatConversationDTO> result = chatService.getConversations("X001", "admin");

        assertTrue(result.isEmpty());
    }

    @Test
    void getConversations_nullMonitorName_fallsbackToRepo() {
        Monitor noNameMonitor = new Monitor();
        noNameMonitor.setCode("M001");
        noNameMonitor.setIdMonitor("M001");
        noNameMonitor.setName(null);
        noNameMonitor.setLastName(null);
        when(monitorRepository.findAll()).thenReturn(List.of(noNameMonitor));
        when(monitorRepository.findById("M001")).thenReturn(Optional.of(sampleMonitor));
        when(departmentHeadRepository.findAll()).thenReturn(List.of(sampleHead));

        List<ChatConversationDTO> result = chatService.getConversations("P001", "professor");

        assertEquals(2, result.size());
    }

    @Test
    void getConversations_nullProfessorName_fallsbackToRepo() {
        Professor noNameProf = new Professor();
        noNameProf.setId("P002");
        noNameProf.setName(null);
        when(professorRepository.findAll()).thenReturn(List.of(noNameProf));
        when(professorRepository.findById("P002")).thenReturn(Optional.of(sampleProfessor));

        List<ChatConversationDTO> result = chatService.getConversations("M001", "monitor");

        assertEquals(1, result.size());
    }

    // ---- GET MESSAGES ----

    @Test
    void getMessages_returnsList() {
        ChatMessage msg = new ChatMessage();
        msg.setId(1L);
        msg.setConversationId("prof-P001__mon-M001");
        msg.setMessage("Hola");
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc("prof-P001__mon-M001"))
                .thenReturn(List.of(msg));

        List<ChatMessageDTO> result = chatService.getMessages("prof-P001__mon-M001");

        assertEquals(1, result.size());
        assertEquals("Hola", result.get(0).getMessage());
    }

    @Test
    void getMessages_empty_returnsEmpty() {
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc("nonexistent"))
                .thenReturn(List.of());

        List<ChatMessageDTO> result = chatService.getMessages("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void getMessages_tablesNotAvailable_returnsEmpty() {
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc("conv1"))
                .thenThrow(new RuntimeException("Table not found"));

        List<ChatMessageDTO> result = chatService.getMessages("conv1");

        assertTrue(result.isEmpty());
    }

    // ---- SEND MESSAGE ----

    @Test
    void sendMessage_withTextOnly() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId("prof-P001__mon-M001");
        payload.setSenderId("P001");
        payload.setSenderRole("professor");
        payload.setReceiverId("M001");
        payload.setMessage("Hola monitor");

        ChatMessage saved = new ChatMessage();
        saved.setId(1L);
        saved.setConversationId("prof-P001__mon-M001");
        saved.setSenderId("P001");
        saved.setSenderRole("professor");
        saved.setReceiverId("M001");
        saved.setMessage("Hola monitor");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageDTO result = chatService.sendMessage(payload, null);

        assertEquals("Hola monitor", result.getMessage());
        assertEquals("P001", result.getSenderId());
    }

    @Test
    void sendMessage_withFiles() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId("prof-P001__mon-M001");
        payload.setSenderId("P001");
        payload.setSenderRole("professor");
        payload.setReceiverId("M001");
        payload.setMessage("Con archivo");

        ChatMessage saved = new ChatMessage();
        saved.setId(1L);
        saved.setConversationId("prof-P001__mon-M001");
        saved.setSenderId("P001");
        saved.setSenderRole("professor");
        saved.setReceiverId("M001");
        saved.setMessage("Con archivo");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatStorageService.StoredChatAttachment stored =
                new ChatStorageService.StoredChatAttachment("message-1/stamp_doc.pdf", "doc.pdf", null);
        when(chatStorageService.store(anyLong(), any(MultipartFile.class))).thenReturn(stored);

        MultipartFile file = mock(MultipartFile.class);

        ChatMessageDTO result = chatService.sendMessage(payload, List.of(file));

        assertEquals("Con archivo", result.getMessage());
        verify(chatAttachmentRepository).save(any(ChatAttachment.class));
    }

    @Test
    void sendMessage_skipsEmptyFileInList() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());

        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId("prof-P001__mon-M001");
        payload.setSenderId("P001");
        payload.setSenderRole("professor");
        payload.setReceiverId("M001");
        payload.setMessage("Texto con archivo vacío");

        ChatMessage saved = new ChatMessage();
        saved.setId(1L);
        saved.setConversationId("prof-P001__mon-M001");
        saved.setSenderId("P001");
        saved.setSenderRole("professor");
        saved.setReceiverId("M001");
        saved.setMessage("Texto con archivo vacío");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);

        ChatMessageDTO result = chatService.sendMessage(payload, List.of(emptyFile));

        assertEquals("Texto con archivo vacío", result.getMessage());
        verify(chatAttachmentRepository, never()).save(any());
    }

    @Test
    void sendMessage_payloadNull_throwsException() {
        Exception e = assertThrows(Exception.class, () -> chatService.sendMessage(null, null));
        assertTrue(e.getMessage().contains("Payload de mensaje requerido"));
    }

    @Test
    void sendMessage_missingSenderId_throwsException() {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setSenderId(null);
        payload.setSenderRole("professor");

        Exception e = assertThrows(Exception.class, () -> chatService.sendMessage(payload, null));
        assertTrue(e.getMessage().contains("senderId es requerido"));
    }

    @Test
    void sendMessage_missingSenderRole_throwsException() {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setSenderId("P001");
        payload.setSenderRole(null);

        Exception e = assertThrows(Exception.class, () -> chatService.sendMessage(payload, null));
        assertTrue(e.getMessage().contains("senderRole es requerido"));
    }

    @Test
    void sendMessage_noTextNoFiles_throwsException() {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setSenderId("P001");
        payload.setSenderRole("professor");
        payload.setMessage("");

        Exception e = assertThrows(Exception.class, () -> chatService.sendMessage(payload, null));
        assertTrue(e.getMessage().contains("debe incluir texto o al menos un archivo adjunto"));
    }

    @Test
    void sendMessage_resolveConversation_professorToMonitorByReceiverId() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(monitorRepository.findByIdMonitor("M001")).thenReturn(Optional.of(sampleMonitor));

        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId(null);
        payload.setSenderId("P001");
        payload.setSenderRole("professor");
        payload.setReceiverId("M001");
        payload.setMessage("Hola");

        ChatMessage saved = new ChatMessage();
        saved.setId(1L);
        saved.setConversationId("prof-P001__mon-M001");
        saved.setSenderId("P001");
        saved.setSenderRole("professor");
        saved.setReceiverId("M001");
        saved.setMessage("Hola");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageDTO result = chatService.sendMessage(payload, null);

        assertEquals("Hola", result.getMessage());
        verify(chatMessageRepository).save(argThat(m ->
                "prof-P001__mon-M001".equals(m.getConversationId())));
    }

    @Test
    void sendMessage_resolveConversation_professorToHeadByReceiverId() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(departmentHeadRepository.existsById("H001")).thenReturn(true);

        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId(null);
        payload.setSenderId("P001");
        payload.setSenderRole("professor");
        payload.setReceiverId("H001");
        payload.setMessage("Hola jefe");

        ChatMessage saved = new ChatMessage();
        saved.setId(1L);
        saved.setConversationId("head-H001__prof-P001");
        saved.setSenderId("P001");
        saved.setSenderRole("professor");
        saved.setReceiverId("H001");
        saved.setMessage("Hola jefe");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageDTO result = chatService.sendMessage(payload, null);

        assertEquals("Hola jefe", result.getMessage());
        verify(chatMessageRepository).save(argThat(m ->
                "head-H001__prof-P001".equals(m.getConversationId())));
    }

    @Test
    void sendMessage_resolveConversation_monitorToProfessor() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());

        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId(null);
        payload.setSenderId("M001");
        payload.setSenderRole("monitor");
        payload.setReceiverId("P001");
        payload.setMessage("Hola profe");

        ChatMessage saved = new ChatMessage();
        saved.setId(1L);
        saved.setConversationId("prof-P001__mon-M001");
        saved.setSenderId("M001");
        saved.setSenderRole("monitor");
        saved.setReceiverId("P001");
        saved.setMessage("Hola profe");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageDTO result = chatService.sendMessage(payload, null);

        assertEquals("Hola profe", result.getMessage());
    }

    @Test
    void sendMessage_resolveConversation_headToProfessor() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());
        when(professorRepository.existsById("P001")).thenReturn(true);

        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId(null);
        payload.setSenderId("H001");
        payload.setSenderRole("jfedpto");
        payload.setReceiverId("P001");
        payload.setMessage("Hola profe");

        ChatMessage saved = new ChatMessage();
        saved.setId(1L);
        saved.setConversationId("head-H001__prof-P001");
        saved.setSenderId("H001");
        saved.setSenderRole("jfedpto");
        saved.setReceiverId("P001");
        saved.setMessage("Hola profe");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageDTO result = chatService.sendMessage(payload, null);

        assertEquals("Hola profe", result.getMessage());
    }

    @Test
    void sendMessage_resolveConversation_invalidRole_throwsException() {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setSenderId("P001");
        payload.setSenderRole("admin");
        payload.setReceiverId("M001");
        payload.setMessage("Hola");

        Exception e = assertThrows(Exception.class, () -> chatService.sendMessage(payload, null));
        assertTrue(e.getMessage().contains("Rol no soportado"));
    }

    @Test
    void sendMessage_resolveConversation_professorInvalidReceiver_throwsException() {
        when(monitorRepository.findByIdMonitor("X001")).thenReturn(Optional.empty());
        when(departmentHeadRepository.existsById("X001")).thenReturn(false);

        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId(null);
        payload.setSenderId("P001");
        payload.setSenderRole("professor");
        payload.setReceiverId("X001");
        payload.setMessage("Hola");

        Exception e = assertThrows(Exception.class, () -> chatService.sendMessage(payload, null));
        assertTrue(e.getMessage().contains("receiverId inválido para rol professor"));
    }

    // ---- DOWNLOAD ATTACHMENT ----

    @Test
    void downloadAttachment_found_returnsAttachment() throws Exception {
        ChatAttachment attachment = new ChatAttachment();
        attachment.setId(1L);
        attachment.setOriginalName("doc.pdf");
        attachment.setContentType("application/pdf");
        attachment.setStoragePath("message-1/doc.pdf");

        when(chatAttachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));
        when(chatStorageService.loadAsResource("message-1/doc.pdf")).thenReturn(new ByteArrayResource("data".getBytes()));

        ChatService.AttachmentDownload result = chatService.downloadAttachment(1L);

        assertNotNull(result);
        assertEquals("doc.pdf", result.filename());
        assertEquals("application/pdf", result.contentType());
    }

    @Test
    void downloadAttachment_notFound_throwsException() {
        when(chatAttachmentRepository.findById(99L)).thenReturn(Optional.empty());

        Exception e = assertThrows(Exception.class, () -> chatService.downloadAttachment(99L));
        assertEquals("Adjunto no encontrado", e.getMessage());
    }

    @Test
    void downloadAttachment_nullContentType_probesContentType() throws Exception {
        ChatAttachment attachment = new ChatAttachment();
        attachment.setId(2L);
        attachment.setOriginalName("doc.pdf");
        attachment.setContentType(null);
        attachment.setStoragePath("message-2/doc.pdf");

        when(chatAttachmentRepository.findById(2L)).thenReturn(Optional.of(attachment));
        when(chatStorageService.loadAsResource("message-2/doc.pdf"))
                .thenReturn(new ByteArrayResource("data".getBytes()));

        ChatService.AttachmentDownload result = chatService.downloadAttachment(2L);

        assertNotNull(result);
        assertEquals("doc.pdf", result.filename());
        assertTrue(result.contentType().contains("octet-stream") || result.contentType().contains("pdf"));
    }

    // ---- BUILD NAME FALLBACKS ----

    @Test
    void getConversations_nullDepartmentHeadName_fallsback() {
        DepartmentHead noNameHead = new DepartmentHead();
        noNameHead.setId("H002");
        noNameHead.setName(null);
        when(monitorRepository.findAll()).thenReturn(List.of(sampleMonitor));
        when(departmentHeadRepository.findAll()).thenReturn(List.of(noNameHead));

        List<ChatConversationDTO> result = chatService.getConversations("P001", "professor");

        assertEquals(2, result.size());
    }

    @Test
    void getConversations_professorSortingException_doesNotFail() {
        when(monitorRepository.findAll()).thenReturn(List.of(sampleMonitor));
        when(departmentHeadRepository.findAll()).thenReturn(List.of(sampleHead));
        when(chatMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(anyString()))
                .thenThrow(new RuntimeException("Tables not available"));

        List<ChatConversationDTO> result = chatService.getConversations("P001", "professor");

        assertEquals(2, result.size());
    }
}
