package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.dto.ChatMessageCreateDTO;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ChatFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void sendAndRetrieveMessages_keepsHistoryAndActivityReference() throws Exception {
        String conversationId = "prof-P_IT__mon-M_IT";

        ChatMessageCreateDTO first = new ChatMessageCreateDTO();
        first.setConversationId(conversationId);
        first.setSenderId("P_IT");
        first.setSenderRole("professor");
        first.setReceiverId("M_IT");
        first.setActivityId(88);
        first.setMessage("Mensaje 1");

        ChatMessageCreateDTO second = new ChatMessageCreateDTO();
        second.setConversationId(conversationId);
        second.setSenderId("M_IT");
        second.setSenderRole("monitor");
        second.setReceiverId("P_IT");
        second.setActivityId(88);
        second.setMessage("Mensaje 2");

        mockMvc.perform(post("/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId", is(conversationId)))
                .andExpect(jsonPath("$.activityId", is(88)))
                .andExpect(jsonPath("$.message", is("Mensaje 1")));

        mockMvc.perform(post("/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId", is(conversationId)))
                .andExpect(jsonPath("$.activityId", is(88)))
                .andExpect(jsonPath("$.message", is("Mensaje 2")));

        mockMvc.perform(get("/chat/messages/{conversationId}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].message", is("Mensaje 1")))
                .andExpect(jsonPath("$[0].activityId", is(88)))
                .andExpect(jsonPath("$[1].message", is("Mensaje 2")))
                .andExpect(jsonPath("$[1].activityId", is(88)));
    }

    @Test
    void sendAndRetrieveMessages_departmentHeadAndProfessor_keepsBidirectionalHistory() throws Exception {
        String conversationId = "head-H_IT__prof-P_IT";

        ChatMessageCreateDTO first = new ChatMessageCreateDTO();
        first.setConversationId(conversationId);
        first.setSenderId("H_IT");
        first.setSenderRole("jfedpto");
        first.setReceiverId("P_IT");
        first.setMessage("Mensaje jefe a profesor");

        ChatMessageCreateDTO second = new ChatMessageCreateDTO();
        second.setConversationId(conversationId);
        second.setSenderId("P_IT");
        second.setSenderRole("professor");
        second.setReceiverId("H_IT");
        second.setMessage("Respuesta profesor a jefe");

        mockMvc.perform(post("/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId", is(conversationId)))
                .andExpect(jsonPath("$.senderRole", is("jfedpto")));

        mockMvc.perform(post("/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId", is(conversationId)))
                .andExpect(jsonPath("$.senderRole", is("professor")));

        mockMvc.perform(get("/chat/messages/{conversationId}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].message", is("Mensaje jefe a profesor")))
                .andExpect(jsonPath("$[0].senderRole", is("jfedpto")))
                .andExpect(jsonPath("$[1].message", is("Respuesta profesor a jefe")))
                .andExpect(jsonPath("$[1].senderRole", is("professor")));
    }
}
