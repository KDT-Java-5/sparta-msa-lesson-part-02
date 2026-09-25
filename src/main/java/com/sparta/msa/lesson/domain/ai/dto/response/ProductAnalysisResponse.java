package com.sparta.msa.lesson.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@NoArgsConstructor // JSON 역직렬화를 위해 기본 생성자 필수
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAnalysisResponse {

  @JsonProperty("sentiment")
  String sentiment;

  @JsonProperty("score")
  int score;

  @JsonProperty("summary")
  String summary;
}
