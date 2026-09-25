package com.sparta.msa.lesson.domain.ai.controller;

import com.sparta.msa.lesson.domain.ai.dto.request.ContextChatRequest;
import com.sparta.msa.lesson.domain.ai.dto.response.ContextChatResponse;
import com.sparta.msa.lesson.domain.ai.service.GeminiChatService;
import com.sparta.msa.lesson.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/gemini")
public class GeminiChatController {

  private final GeminiChatService chatService;

  // 단발성 채팅 (히스토리 없음) 맥락 유지 없이 일회성 질문에 대한 답변을 제공합니다.
  @PostMapping("/simple")
  public ApiResponse<ContextChatResponse> simpleChat(@RequestBody ContextChatRequest request) {
    return ApiResponse.ok(chatService.chat(request.getMessage()));
  }

  // 대화 히스토리 유지 채팅 (핵심 기능) conversationId를 통해 과거 대화 맥락을 포함한 답변을 제공합니다.
  @PostMapping
  public ApiResponse<ContextChatResponse> chat(@RequestBody ContextChatRequest request) {
    return ApiResponse.ok(
        chatService.chatWithHistory(request.getMessage(), request.getConversationId()));
  }

  // 스트리밍 채팅 (Server-Sent Events) 답변이 생성되는 대로 실시간으로 클라이언트에 전송합니다.
  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> streamChat(@RequestBody ContextChatRequest request) {
    return chatService.chatStream(request.getMessage());
  }

  // 모든 대화 메모리 초기화
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ApiResponse<Void> deleteAllConversations() {
    chatService.clearAll();
    return ApiResponse.ok();
  }

}
