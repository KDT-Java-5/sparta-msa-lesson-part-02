package com.sparta.msa.lesson.domain.ai.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContextChatRequest {

  // 사용자가 AI에게 보내는 질문 또는 명령 텍스트
  String message;

  // 대화의 연속성을 유지하기 위한 고유 세션 ID
  // (기존 대화를 이어갈 경우 필수, 처음 시작할 경우 null 가능)
  String conversationId;

}
