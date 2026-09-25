package com.sparta.msa.lesson.domain.ai.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OllamaChatRequest {

  String systemPrompt; // AI 역할 설정 (페르소나)

  String message;      // 사용자 질문 내용

  Double temperature;  // 답변의 창의성 정도 (0.0 ~ 1.0)

}
