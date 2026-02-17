package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.controller.ChatController;
import com.pdg.sigma.dto.ChatConversationDTO;
import com.pdg.sigma.dto.ChatMessageCreateDTO;
import com.pdg.sigma.dto.ChatMessageDTO;
import com.pdg.sigma.service.ChatService;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getConversations_returnsOk() throws Exception {
        List<ChatConversationDTO> conversations = List.of(
                new ChatConversationDTO("prof-P1__mon-M1", "Monitor: Ana", "Chat directo", "M1")
        );
        Mockito.when(chatService.getConversations("P1", "professor")).thenReturn(conversations);

        mockMvc.perform(get("/chat/conversations/{userId}/{role}", "P1", "professor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("prof-P1__mon-M1")));
    }

    @Test
    void sendMessage_returnsCreated() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId("prof-P1__mon-M1");
        payload.setSenderId("P1");
        payload.setSenderRole("professor");
        payload.setReceiverId("M1");
        payload.setMessage("Hola");

        ChatMessageDTO created = new ChatMessageDTO(buildMessage(7L, "prof-P1__mon-M1", "P1", "professor", "M1", "Hola"), List.of());
        Mockito.when(chatService.sendMessage(any(ChatMessageCreateDTO.class), any())).thenReturn(created);

        mockMvc.perform(post("/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.conversationId", is("prof-P1__mon-M1")));
    }

    @Test
    void sendMessageWithAttachments_returnsCreated() throws Exception {
        ChatMessageCreateDTO payload = new ChatMessageCreateDTO();
        payload.setConversationId("prof-P1__mon-M1");
        payload.setSenderId("P1");
        payload.setSenderRole("professor");
        payload.setReceiverId("M1");
        payload.setMessage("Adjunto");

        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload",
                "payload",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(payload)
        );
        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "demo.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "contenido".getBytes()
        );

        ChatMessageDTO created = new ChatMessageDTO(buildMessage(9L, "prof-P1__mon-M1", "P1", "professor", "M1", "Adjunto"), List.of());
        Mockito.when(chatService.sendMessage(any(ChatMessageCreateDTO.class), any())).thenReturn(created);

        mockMvc.perform(multipart("/chat/messages/with-attachments")
                        .file(payloadPart)
                        .file(filePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(9)));
    }

    @Test
    void downloadAttachment_notFound_returns404() throws Exception {
        Mockito.when(chatService.downloadAttachment(999L)).thenThrow(new Exception("Adjunto no encontrado"));

        mockMvc.perform(get("/chat/attachments/{attachmentId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Adjunto no encontrado"));
    }

    @Test
    void downloadAttachment_returnsFile() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("abc".getBytes());
        ChatService.AttachmentDownload download = new ChatService.AttachmentDownload(resource, "nota.txt", "text/plain");
        Mockito.when(chatService.downloadAttachment(eq(1L))).thenReturn(download);

        mockMvc.perform(get("/chat/attachments/{attachmentId}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"));
    }

    private com.pdg.sigma.domain.ChatMessage buildMessage(
            Long id,
            String conversationId,
            String senderId,
            String senderRole,
            String receiverId,
            String text
    ) {
        com.pdg.sigma.domain.ChatMessage message = new com.pdg.sigma.domain.ChatMessage();
        message.setId(id);
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setReceiverId(receiverId);
        message.setMessage(text);
        message.setCreatedAt(new Date());
        return message;
    }
}
