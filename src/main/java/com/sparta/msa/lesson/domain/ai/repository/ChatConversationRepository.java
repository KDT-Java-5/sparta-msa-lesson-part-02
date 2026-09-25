package com.sparta.msa.lesson.domain.ai.repository;

import com.sparta.msa.lesson.domain.ai.entity.ChatConversation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

}
