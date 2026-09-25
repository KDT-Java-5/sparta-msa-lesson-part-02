package com.sparta.msa.lesson.domain.function.service;

import com.sparta.msa.lesson.domain.function.tools.FunctionTools;
import com.sparta.msa.lesson.global.exception.DomainException;
import com.sparta.msa.lesson.global.exception.DomainExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallingService {

  private final ChatClient.Builder clientBuilder;
  private final FunctionTools functionTools;

  /**
   * 기본 Function Calling - 모든 도구 사용 가능
   */
  public String chat(String userMessage) {
    log.info("[Chat] User Message: {}", userMessage);
    try {
      return clientBuilder.build()
          .prompt()
          .user(userMessage)
          .tools(functionTools)
          .call()
          .content();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
  }

  /**
   * 시스템 메시지 설정 및 Function Calling
   */
  public String chatWithSystemMessage(String systemMessage, String userMessage) {
    log.info("[System Chat] System: {}, User: {}", systemMessage, userMessage);
    try {
      return clientBuilder.build()
          .prompt()
          .system(systemMessage)
          .user(userMessage)
          .tools(functionTools)
          .call()
          .content();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
  }

}