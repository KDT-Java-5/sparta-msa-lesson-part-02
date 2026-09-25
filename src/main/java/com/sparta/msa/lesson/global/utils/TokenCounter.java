package com.sparta.msa.lesson.global.utils;

import java.util.List;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.stereotype.Component;

@Component
public class TokenCounter {

  // 단일 문자열의 토큰 수 계산
  public static Integer countTokens(String text) {
    // JTokkit: OpenAI의 tiktoken 라이브러리를 자바에서 쓸 수 있게 만든 도구
    TokenCountEstimator estimator = new JTokkitTokenCountEstimator();
    return estimator.estimate(text);
  }

  // 대화 히스토리 등 여러 메시지의 총 토큰 수 계산
  public static Integer countTokens(List<String> messages) {
    TokenCountEstimator estimator = new JTokkitTokenCountEstimator();
    return messages.stream()
        .mapToInt(estimator::estimate)
        .sum();
  }
}
