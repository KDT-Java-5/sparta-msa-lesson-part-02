package com.sparta.msa.lesson.domain.ai.service;

import com.sparta.msa.lesson.domain.ai.dto.response.ContextChatResponse;
import com.sparta.msa.lesson.domain.ai.entity.ChatConversation;
import com.sparta.msa.lesson.domain.ai.entity.ChatMessage;
import com.sparta.msa.lesson.domain.ai.repository.ChatConversationRepository;
import com.sparta.msa.lesson.domain.ai.repository.ChatMessageRepository;
import com.sparta.msa.lesson.global.constants.enums.ChatMessageType;
import com.sparta.msa.lesson.global.constants.enums.StatusType;
import com.sparta.msa.lesson.global.exception.DomainException;
import com.sparta.msa.lesson.global.exception.DomainExceptionCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistentChatService {

  private final ChatClient chatClient;
  private final ChatConversationRepository chatConversationRepository;
  private final ChatMessageRepository chatMessageRepository;

  private static final int MAX_HISTORY_MESSAGES = 5;

  @Transactional
  public ContextChatResponse chat(String conversationId, String userMessage) {
    ChatConversation conversation;
    if (!StringUtils.hasText(conversationId)) {
      conversation = saveConversation(userMessage);
    } else {
      conversation = chatConversationRepository.findById(UUID.fromString(conversationId))
          .orElseThrow(() -> new DomainException(DomainExceptionCode.NOT_FOUND_CONVERSATION));
    }

    saveMessage(conversation, userMessage, ChatMessageType.USER, null);

    List<ChatMessage> persistentMessages =
        chatMessageRepository.findByConversation_IdAndStatus(conversation.getId(),
            StatusType.ACTIVE);

    List<Message> recentMessages = convertToMessages(persistentMessages, conversation);

    try {
      ChatResponse response = chatClient.prompt()
          .messages(recentMessages)
          .call()
          .chatResponse();

      String assistantResponse = response.getResult().getOutput().getText();
      Usage usage = response.getMetadata().getUsage();

      saveMessage(conversation, assistantResponse, ChatMessageType.ASSISTANT, usage);

      ContextChatResponse.TokenUsage tokenUsage = ContextChatResponse.TokenUsage.builder()
          .promptTokens(usage.getPromptTokens())
          .completionTokens(usage.getCompletionTokens())
          .totalTokens(usage.getTotalTokens())
          .build();

      return ContextChatResponse.builder()
          .message(assistantResponse)
          .conversationId(conversation.getId().toString())
          .timestamp(LocalDateTime.now())
          .tokenUsage(tokenUsage)
          .build();

    } catch (Exception e) {
      log.error("AI 실행 중 오류 발생: {}", e.getMessage());
      throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
  }

  private ChatConversation saveConversation(String userMessage) {
    String title = userMessage.length() > 50 ?
        userMessage.substring(0, 50) + "..." : userMessage;

    return chatConversationRepository.save(
        ChatConversation.builder()
            .id(UUID.randomUUID())
            .title(title)
            .build());
  }

  private ChatMessage saveMessage(ChatConversation conversation, String message,
      ChatMessageType role, Usage usage) {
    return chatMessageRepository.save(ChatMessage.builder()
        .conversation(conversation)
        .role(role)
        .status(StatusType.ACTIVE)
        .message(message)
        .promptTokens(ObjectUtils.isEmpty(usage) ? null : usage.getPromptTokens())
        .completionTokens(ObjectUtils.isEmpty(usage) ? null : usage.getCompletionTokens())
        .totalTokens(ObjectUtils.isEmpty(usage) ? null : usage.getTotalTokens())
        .build());
  }

  private List<Message> convertToMessages(List<ChatMessage> messages,
      ChatConversation conversation) {
    if (messages == null || messages.isEmpty()) {
      return List.of();
    }

    int start = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);

    if (start > 0) {
      List<ChatMessage> limitedMessages = messages.subList(start, messages.size());
      String summary = generateSummary(messages.subList(0, start));

      messages.forEach(m -> m.setStatus(StatusType.INACTIVE));
      chatMessageRepository.saveAll(messages);

      ChatMessage summaryMessage = saveSummaryMessage(conversation, summary);

      List<ChatMessage> result = new ArrayList<>();
      result.add(summaryMessage);
      result.addAll(limitedMessages);

      return result.stream()
          .map(this::mapToSpringAiMessage)
          .toList();
    }

    return messages.stream()
        .map(this::mapToSpringAiMessage)
        .toList();
  }

  private Message mapToSpringAiMessage(ChatMessage entity) {
    String content = entity.getMessage();

    return switch (entity.getRole()) {
      case USER -> new org.springframework.ai.chat.messages.UserMessage(content);
      case ASSISTANT -> new org.springframework.ai.chat.messages.AssistantMessage(content);
      case SYSTEM, SUMMARY -> new org.springframework.ai.chat.messages.SystemMessage(content);
      default -> throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    };
  }

  private String generateSummary(List<ChatMessage> messages) {
    String conversationText = messages.stream()
        .map(m -> m.getRole().name() + ": " + m.getMessage())
        .collect(Collectors.joining("\n"));

    String prompt = """
        다음 대화 내용을 핵심 정보 위주로 간결하게 요약해줘.
        요약은 향후 대화의 맥락으로 사용될 거야.
        
        대화 내용:
        %s
        """.formatted(conversationText);

    try {
      return chatClient.prompt()
          .user(prompt)
          .call()
          .content();
    } catch (Exception e) {
      log.error("대화 요약 생성 실패: {}", e.getMessage());
      throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
  }

  private ChatMessage saveSummaryMessage(ChatConversation conversation, String summary) {
    return chatMessageRepository.save(ChatMessage.builder()
        .conversation(conversation)
        .role(ChatMessageType.SUMMARY)
        .status(StatusType.ACTIVE)
        .message(summary)
        .build());
  }

}
