package com.sparta.msa.lesson.domain.ai.controller;

import com.sparta.msa.lesson.domain.ai.dto.response.ProductAnalysisResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

  private final ChatClient chatClient;

  // 생성자 주입을 통한 ChatClient 빌드
  public AiChatController(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  @PostMapping("/chat")
  public String chat(@RequestBody String message) {
    return chatClient.prompt()
        .user(message)
        .call()
        .content(); // 문자열로 결과 반환
  }

  @GetMapping("/marketing")
  public String generateMarketing(
      @RequestParam(value = "productName") String productName,
      @RequestParam(value = "features") String features) {

    String template = """
        제품명 {productName}의 마케팅 문구를 작성하세요.
        주요 특징: {features}
        조건: 감성적이고 100자 이내로 작성할 것.
        """;

    return chatClient.prompt()
        .user(u -> u.text(template)
            .param("productName", productName)
            .param("features", features))
        .call()
        .content();
  }

  @GetMapping("/translate")
  public String translate(
      @RequestParam(value = "text") String text,
      @RequestParam(value = "targetLanguage", defaultValue = "영어") String targetLanguage) {

    return chatClient.prompt()
        // 1. AI의 페르소나 설정 (System Message)
        .system("당신은 전문 번역가입니다. 주어진 텍스트를 문맥에 맞게 자연스럽게 번역해주세요.")

        // 2. 동적 파라미터 주입 (Prompt Template)
        .user(u -> u.text("다음 텍스트를 {lang}로 번역해주세요: {text}")
            .param("lang", targetLanguage)
            .param("text", text))
        .call()
        .content();
  }

  @GetMapping("/analyze")
  public ProductAnalysisResponse analyzeReview(@RequestParam(value = "review") String review) {

    String promptText = """
        다음 제품 리뷰를 분석해주세요:
        
        리뷰 내용: {review}
        
        요구사항:
        1. sentiment는 positive, neutral, negative 중 하나로 응답하세요.
        2. score는 1점에서 10점 사이의 정수로 응답하세요.
        3. summary는 분석 내용을 한 문장으로 요약하세요.
        """;

    return chatClient.prompt()
        .user(u -> u.text(promptText).param("review", review))
        .call()
        .entity(ProductAnalysisResponse.class); // 핵심: 객체로 자동 변환
  }

  @GetMapping("/story")
  public String generateStory(@RequestParam(value = "topic") String topic) {
    return chatClient.prompt()
        .user("다음 주제로 창의적인 이야기를 작성해주세요: " + topic)
        .options(ChatOptions.builder()
            .temperature(0.9)  // 1.0에 가까울수록 창의적(랜덤성 증가)
            .maxTokens(500)    // 답변의 최대 길이 제한
            .build())
        .call()
        .content();
  }

  @GetMapping("/summary")
  public String generateSummary(@RequestParam(value = "text") String text) {
    return chatClient.prompt()
        .user("다음 텍스트를 핵심 위주로 요약해주세요: " + text)
        .options(ChatOptions.builder()
            .temperature(0.1)  // 0.0에 가까울수록 일관적이고 사실적
            .maxTokens(200)
            .build())
        .call()
        .content();
  }

}
