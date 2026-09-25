package com.sparta.msa.lesson.domain.ai.repository;

import com.sparta.msa.lesson.domain.ai.entity.ChatMessage;
import com.sparta.msa.lesson.global.constants.enums.StatusType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  List<ChatMessage> findByConversation_IdAndStatus(UUID chatConversationId, StatusType status);

}
