package com.sparta.msa.lesson.domain.ai.controller;

import com.sparta.msa.lesson.domain.ai.dto.request.ContextChatRequest;
import com.sparta.msa.lesson.domain.ai.dto.response.ContextChatResponse;
import com.sparta.msa.lesson.domain.ai.dto.response.ImageAnalysisResponse;
import com.sparta.msa.lesson.domain.ai.service.GeminiChatService;
import com.sparta.msa.lesson.domain.ai.service.PersistentChatService;
import com.sparta.msa.lesson.domain.ai.service.VisionChatService;
import com.sparta.msa.lesson.global.response.ApiResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/gemini")
public class GeminiChatController {

  private final GeminiChatService chatService;
  private final PersistentChatService persistentChatService;
  private final VisionChatService visionChatService;

  // 단발성 채팅 (히스토리 없음) 맥락 유지 없이 일회성 질문에 대한 답변을 제공합니다.
  @PostMapping("/simple")
  public ApiResponse<ContextChatResponse> simpleChat(@RequestBody ContextChatRequest request) {
    return ApiResponse.ok(chatService.chat(request.getMessage()));
  }

  // 대화 히스토리 유지 채팅 (핵심 기능) conversationId를 통해 과거 대화 맥락을 포함한 답변을 제공합니다.
  @PostMapping
  public ApiResponse<ContextChatResponse> chat(@RequestBody ContextChatRequest request) {
    return ApiResponse.ok(
        persistentChatService.chat(request.getConversationId(), request.getMessage()));
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

  @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<ImageAnalysisResponse> analyzeImage(
      @RequestParam String message,
      @RequestParam MultipartFile image) throws IOException {
    return ApiResponse.ok(visionChatService.analyzeImage(message, image));
  }

}
