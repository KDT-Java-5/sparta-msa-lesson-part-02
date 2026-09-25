package com.sparta.msa.lesson.domain.ai.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageAnalysisResponse {

  String analysis;

  String imageType;

  Long imageSize;

  TokenUsage tokenUsage;

  @Getter
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class TokenUsage {

    Integer promptTokens;

    Integer completionTokens;

    Integer totalTokens;
  }
}
