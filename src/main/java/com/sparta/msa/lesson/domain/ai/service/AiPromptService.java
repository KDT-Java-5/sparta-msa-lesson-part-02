package com.sparta.msa.lesson.domain.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiPromptService {

  private final ChatClient chatClient;

  public AiPromptService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String generateProductDescription(String productName, String features) {
    String template = """
        제품명 {productName}의 마케팅 문구를 작성하세요.
        주요 특징: {features}
        조건: 감성적이고 100자 이내로 작성할 것.
        """;

    return chatClient.prompt()
        .user(u -> u.text(template)
            .param("productName", productName)
            .param("features", features))
        .call()
        .content();
  }
}