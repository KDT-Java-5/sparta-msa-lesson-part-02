package com.sparta.msa.lesson.domain.function.controller;

import com.sparta.msa.lesson.domain.function.dto.request.QuestionRequest;
import com.sparta.msa.lesson.domain.function.dto.response.AnswerResponse;
import com.sparta.msa.lesson.domain.function.service.FunctionCallingService;
import com.sparta.msa.lesson.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/function")
public class FunctionCallingController {

  private final FunctionCallingService functionCallingService;

  /**
   * 기본 Function Calling 채팅 인터페이스
   */
  @PostMapping("/chat")
  public ApiResponse<AnswerResponse> chat(@RequestBody QuestionRequest request) {
    String result = functionCallingService.chat(request.getQuestion());
    return ApiResponse.ok(AnswerResponse.builder().answer(result).build());
  }

}