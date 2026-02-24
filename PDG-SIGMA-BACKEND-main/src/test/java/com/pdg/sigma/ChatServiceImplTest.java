package com.pdg.sigma;

import com.pdg.sigma.domain.ChatMessage;
import com.pdg.sigma.dto.ChatMessageCreateDTO;
import com.pdg.sigma.dto.ChatMessageDTO;
import com.pdg.sigma.repository.ChatAttachmentRepository;
import com.pdg.sigma.repository.ChatMessageRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.ProfessorRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private ProfessorRepository professorRepository;

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
}