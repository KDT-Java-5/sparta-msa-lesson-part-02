package com.sparta.msa.lesson.domain.ai.service;

import com.sparta.msa.lesson.domain.ai.dto.response.ContextChatResponse;
import com.sparta.msa.lesson.global.exception.DomainException;
import com.sparta.msa.lesson.global.exception.DomainExceptionCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiChatService {

  // GeminiChatConfig에서 생성된 ChatClient 주입
  private final ChatClient chatClient;

  // 대화 이력을 관리하기 위한 저장소 (Key: conversationId, Value: 메시지 리스트)
  // 멀티 스레드 환경에서 안전하도록 ConcurrentHashMap 사용
  private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

  // 기본 채팅 (히스토리 없음) 질문 하나에 답변 하나만 제공하며 맥락을 유지하지 않습니다.
  public ContextChatResponse chat(String question) {
    String responseContent = chatClient.prompt()
        .user(question)
        .call()
        .content();

    return ContextChatResponse.builder()
        .message(responseContent)
        .conversationId(UUID.randomUUID().toString()) // 새 세션 ID 부여
        .timestamp(LocalDateTime.now())
        .build();
  }

  // 대화 히스토리를 유지하는 채팅 (핵심 기능) 과거 대화 내용을 포함하여 질문을 던짐으로써 맥락 있는 답변을 유도합니다.
  public ContextChatResponse chatWithHistory(String question, String conversationId) {
    // ID가 없으면 새로 생성
    if (conversationId == null || conversationId.isBlank()) {
      conversationId = UUID.randomUUID().toString();
    }

    // 기존 대화 이력을 가져오거나 새로 생성
    List<Message> history = conversations.getOrDefault(conversationId, new ArrayList<>());

    // 사용자 질문 추가
    UserMessage userMessage = new UserMessage(question);
    history.add(userMessage);

    try {
      // AI 호출: 지금까지의 history 전체를 메시지로 전달
      ChatResponse response = chatClient.prompt()
          .messages(history)
          .call()
          .chatResponse();

      String assistantResponse = response.getResult().getOutput().getText();

      // AI 답변을 히스토리에 추가
      AssistantMessage assistantMessage = new AssistantMessage(assistantResponse);
      history.add(assistantMessage);

      // 업데이트된 히스토리 저장
      conversations.put(conversationId, history);

      // 토큰 사용량 정보 추출 및 DTO 변환
      var usage = response.getMetadata().getUsage();
      ContextChatResponse.TokenUsage tokenUsage = ContextChatResponse.TokenUsage.builder()
          .promptTokens(usage.getPromptTokens().intValue())
          .completionTokens(usage.getCompletionTokens().intValue())
          .totalTokens(usage.getTotalTokens().intValue())
          .build();

      return ContextChatResponse.builder()
          .message(assistantResponse)
          .conversationId(conversationId)
          .timestamp(LocalDateTime.now())
          .tokenUsage(tokenUsage)
          .build();

    } catch (Exception e) {
      log.error("AI 호출 중 오류 발생: {}", e.getMessage());
      throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
  }

  // 스트리밍 채팅 답변을 실시간으로 한 글자씩 끊어서 반환합니다.
  public Flux<String> chatStream(String question) {
    return chatClient.prompt()
        .user(question)
        .stream()
        .content();
  }

  // 모든 대화 메모리 초기화
  public void clearAll() {
    conversations.clear();
    log.info("모든 대화 세션 초기화 완료");
  }

}
