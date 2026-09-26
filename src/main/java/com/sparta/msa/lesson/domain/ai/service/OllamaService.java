package com.sparta.msa.lesson.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OllamaService {

  private final ChatClient chatClient;

  // 1. 단순 채팅
  public String chat(String message) {
    return chatClient.prompt()
        .user(message)
        .call()
        .content();
  }

  // 2. 역할 기반 및 옵션 조정 채팅 (System Prompt & Temperature)
  public String chatWithOptions(String systemPrompt, String message, Double temperature) {
    return chatClient.prompt()
        .system(systemPrompt)
        .user(message)
        .options(OpenAiChatOptions.builder()
            .temperature(temperature))
        .call()
        .content();
  }
}
