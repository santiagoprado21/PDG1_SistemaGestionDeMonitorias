package com.pdg.sigma.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pdg.sigma.domain.ChatAttachment;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

    List<ChatAttachment> findByMessageIdOrderByIdAsc(Long messageId);
}
