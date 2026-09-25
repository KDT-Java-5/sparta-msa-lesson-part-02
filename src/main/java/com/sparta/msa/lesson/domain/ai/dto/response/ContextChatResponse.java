package com.sparta.msa.lesson.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContextChatResponse {

  // AI가 생성한 최종 답변 텍스트
  String message;

  // 대화의 연속성을 식별하기 위한 고유 세션 ID (이후 요청 시 이 ID를 전달하여 대화 문맥을 유지함)
  String conversationId;

  // 서버에서 응답이 생성된 시각
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime timestamp;

  // 이번 API 호출에서 발생한 상세 토큰 사용량 정보
  TokenUsage tokenUsage;

  @Getter
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class TokenUsage {

    // 질문 및 과거 대화 내역을 포함한 입력(Input) 토큰 수
    Integer promptTokens;

    // AI가 답변을 생성하며 소비한 출력(Output) 토큰 수
    Integer completionTokens;

    // 입력과 출력을 합산한 총 토큰 수 (실제 과금 및 비용 계산의 기준)
    Integer totalTokens;
  }
}
