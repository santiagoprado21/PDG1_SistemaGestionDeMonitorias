package com.pdg.sigma.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pdg.sigma.domain.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    Optional<ChatMessage> findTopByConversationIdOrderByCreatedAtDesc(String conversationId);
}
