package com.sparta.msa.lesson.domain.ai.controller;

import com.sparta.msa.lesson.domain.ai.dto.request.ChatRequest;
import com.sparta.msa.lesson.domain.ai.dto.response.ChatResponse;
import com.sparta.msa.lesson.domain.ai.service.ChatService;
import com.sparta.msa.lesson.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-chat")
public class ChatController {

  private final ChatService chatService;

  @PostMapping
  public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request) {
    return ApiResponse.ok(chatService.chat(request.getMessage()));
  }

  @PostMapping("/context")
  public ApiResponse<ChatResponse> context(@RequestBody ChatRequest request) {
    return ApiResponse.ok(chatService.chatWithContext(request.getMessage()));
  }

}
