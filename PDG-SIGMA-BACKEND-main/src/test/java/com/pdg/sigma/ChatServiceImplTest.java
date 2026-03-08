package com.pdg.sigma;

import com.pdg.sigma.domain.ChatMessage;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.dto.ChatMessageCreateDTO;
import com.pdg.sigma.dto.ChatConversationDTO;
import com.pdg.sigma.dto.ChatMessageDTO;
import com.pdg.sigma.repository.ChatAttachmentRepository;
import com.pdg.sigma.repository.ChatMessageRepository;
import com.pdg.sigma.repository.DepartmentHeadRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.service.DepartmentHeadService;
import com.pdg.sigma.service.ChatServiceImpl;
import com.pdg.sigma.service.ChatStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void sendMessage_professorWithoutConversation_buildsConversationAndPersists() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setSenderId("PROF1");
        payload.setSenderRole(" Professor ");
        payload.setReceiverId("MON1");
        payload.setMessage("  hola monitor  ");

        Monitor monitor = new Monitor();
        monitor.setIdMonitor("MON1");
        when(monitorRepository.findByIdMonitor("MON1")).thenReturn(Optional.of(monitor));

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(10L);
            message.setCreatedAt(new Date());
            return message;
        });
        when(chatAttachmentRepository.findByMessageIdOrderByIdAsc(10L)).thenReturn(List.of());

        ChatMessageDTO result = chatService.sendMessage(payload, List.of());

        assertThat(result.getConversationId()).isEqualTo("prof-PROF1__mon-MON1");
        assertThat(result.getSenderRole()).isEqualTo("professor");
        assertThat(result.getReceiverId()).isEqualTo("MON1");
        assertThat(result.getMessage()).isEqualTo("hola monitor");
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(jdbcTemplate, atLeast(3)).execute(any(String.class));
    }

    @Test
    void sendMessage_monitorWithoutReceiver_resolvesReceiverFromConversationId() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId("prof-PROF2__mon-MON9");
        payload.setSenderId("MON9");
        payload.setSenderRole("monitor");
        payload.setMessage("mensaje");

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(11L);
            message.setCreatedAt(new Date());
            return message;
        });
        when(chatAttachmentRepository.findByMessageIdOrderByIdAsc(11L)).thenReturn(List.of());

        ChatMessageDTO result = chatService.sendMessage(payload, List.of());

        assertThat(result.getReceiverId()).isEqualTo("PROF2");
        assertThat(result.getConversationId()).isEqualTo("prof-PROF2__mon-MON9");
        verify(chatAttachmentRepository).findByMessageIdOrderByIdAsc(eq(11L));
    }

    @Test
    void sendMessage_withActivityId_keepsActivityReference() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId("prof-PROF3__mon-MON3");
        payload.setSenderId("PROF3");
        payload.setSenderRole("professor");
        payload.setReceiverId("MON3");
        payload.setActivityId(88);
        payload.setMessage("Revisa la actividad 88");

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(12L);
            message.setCreatedAt(new Date());
            return message;
        });
        when(chatAttachmentRepository.findByMessageIdOrderByIdAsc(12L)).thenReturn(List.of());

        ChatMessageDTO result = chatService.sendMessage(payload, List.of());

        assertThat(result.getActivityId()).isEqualTo(88);
        assertThat(result.getMessage()).isEqualTo("Revisa la actividad 88");
    }

    @Test
    void sendMessage_withoutTextAndFiles_throwsValidationError() {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setSenderId("PROF1");
        payload.setSenderRole("professor");
        payload.setReceiverId("MON1");
        payload.setMessage("   ");

        assertThatThrownBy(() -> chatService.sendMessage(payload, List.of()))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("texto o al menos un archivo adjunto");
    }

    @Test
    void getMessages_whenRepositoryThrows_returnsEmptyList() {
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc("conv-1"))
                .thenThrow(new RuntimeException("missing table"));

        List<ChatMessageDTO> result = chatService.getMessages("conv-1");

        assertThat(result).isEmpty();
    }

    @Test
    void getConversations_departmentHead_returnsProfessorConversations() {
        Professor professor = new Professor();
        professor.setId("P100");
        professor.setName("Laura Diaz");

        when(departmentHeadService.getProfessorsByDepartmentHead("H5001")).thenReturn(List.of(professor));
        List<ChatConversationDTO> result = chatService.getConversations("H5001", "jfedpto");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("head-H5001__prof-P100");
        assertThat(result.get(0).getTitle()).isEqualTo("Profesor: Laura Diaz");
        assertThat(result.get(0).getOtherUserId()).isEqualTo("P100");
    }

    @Test
    void sendMessage_departmentHeadWithoutConversation_buildsHeadProfessorConversation() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setSenderId("H5001");
        payload.setSenderRole("jfedpto");
        payload.setReceiverId("P100");
        payload.setMessage("Mensaje del jefe");

        when(professorRepository.existsById("P100")).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(20L);
            message.setCreatedAt(new Date());
            return message;
        });
        when(chatAttachmentRepository.findByMessageIdOrderByIdAsc(20L)).thenReturn(List.of());

        ChatMessageDTO result = chatService.sendMessage(payload, List.of());

        assertThat(result.getConversationId()).isEqualTo("head-H5001__prof-P100");
        assertThat(result.getReceiverId()).isEqualTo("P100");
        assertThat(result.getSenderRole()).isEqualTo("jfedpto");
    }

    @Test
    void sendMessage_professorToDepartmentHeadWithoutConversation_buildsHeadProfessorConversation() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setSenderId("P200");
        payload.setSenderRole("professor");
        payload.setReceiverId("H6001");
        payload.setMessage("Mensaje al jefe");

        when(monitorRepository.findByIdMonitor("H6001")).thenReturn(Optional.empty());
        when(departmentHeadRepository.existsById("H6001")).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(21L);
            message.setCreatedAt(new Date());
            return message;
        });
        when(chatAttachmentRepository.findByMessageIdOrderByIdAsc(21L)).thenReturn(List.of());

        ChatMessageDTO result = chatService.sendMessage(payload, List.of());

        assertThat(result.getConversationId()).isEqualTo("head-H6001__prof-P200");
        assertThat(result.getReceiverId()).isEqualTo("H6001");
    }

    @Test
    void sendMessage_professorFromHeadConversationWithoutReceiver_resolvesDepartmentHeadReceiver() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId("head-H9001__prof-P9001");
        payload.setSenderId("P9001");
        payload.setSenderRole("professor");
        payload.setMessage("Respuesta al jefe");

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(22L);
            message.setCreatedAt(new Date());
            return message;
        });
        when(chatAttachmentRepository.findByMessageIdOrderByIdAsc(22L)).thenReturn(List.of());

        ChatMessageDTO result = chatService.sendMessage(payload, List.of());

        assertThat(result.getReceiverId()).isEqualTo("H9001");
        assertThat(result.getConversationId()).isEqualTo("head-H9001__prof-P9001");
    }
}