package com.sparta.msa.lesson.domain.ai.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

@Slf4j
public class ChatMemoryAdvisor implements BaseAdvisor {

  private static final String CONTEXT_USER_TEXT = "chat_memory_user_text";

  // 대화 저장소 (실제 서비스에선 Redis나 DB로 대체 가능)
  private final Map<String, List<Message>> conversationStore = new ConcurrentHashMap<>();
  private final String conversationId;
  private final int maxMessages;

  public ChatMemoryAdvisor(String conversationId, int maxMessages) {
    this.conversationId = conversationId;
    this.maxMessages = maxMessages;
  }

  @Override
  public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
    // [핵심] 1. 저장소에서 이전 대화 기록을 가져옴
    List<Message> history = conversationStore.getOrDefault(conversationId,
        new CopyOnWriteArrayList<>());

    // [핵심] 2. 이전 기록 + 현재 질문을 합쳐서 새로운 메시지 리스트 생성
    List<Message> fullMessages = new ArrayList<>(history);
    fullMessages.addAll(request.prompt().getInstructions());

    // 3. 현재 질문 텍스트 추출 (나중에 after에서 저장하기 위함)
    String userText = request.prompt().getInstructions().stream()
        .filter(m -> m.getMessageType() == MessageType.USER)
        .map(Message::getText)
        .findFirst().orElse("");

    // 4. 합쳐진 메시지들로 프롬프트를 재구성하여 반환
    return request.mutate()
        .prompt(new Prompt(fullMessages)) // AI에게 과거 기록을 함께 보냄
        .context(CONTEXT_USER_TEXT, userText)
        .build();
  }

  @Override
  public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
    List<Message> history = conversationStore.computeIfAbsent(
        conversationId, k -> new CopyOnWriteArrayList<>()
    );

    // 1. 사용자 질문 저장 (context에서 꺼냄)
    Optional.ofNullable(response.context().get(CONTEXT_USER_TEXT))
        .map(Object::toString)
        .filter(text -> !text.isBlank())
        .ifPresent(text -> history.add(new UserMessage(text)));

    // 2. AI 응답 저장
    if (response.chatResponse() != null && response.chatResponse().getResult() != null) {
      history.add(response.chatResponse().getResult().getOutput());
    }

    // 3. FIFO 용량 관리
    while (history.size() > maxMessages && !history.isEmpty()) {
      history.remove(0);
    }

    return response;
  }

  @Override
  public String getName() {
    return "ChatMemoryAdvisor";
  }

  @Override
  public int getOrder() {
    return 0;
  }
}