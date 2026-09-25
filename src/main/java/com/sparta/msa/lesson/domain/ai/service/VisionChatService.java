package com.sparta.msa.lesson.domain.ai.service;

import com.sparta.msa.lesson.domain.ai.dto.response.ImageAnalysisResponse;
import com.sparta.msa.lesson.global.exception.DomainException;
import com.sparta.msa.lesson.global.exception.DomainExceptionCode;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionChatService {

  private final ChatClient chatClient;

  @Transactional
  public ImageAnalysisResponse analyzeImage(String message, MultipartFile image)
      throws IOException {

    String contentType = image.getContentType();
    if (contentType == null) {
      contentType = "image/jpeg";
    }

    try {
      String finalContentType = contentType;
      var response = chatClient.prompt()
          .user(u -> u
              .text(message)
              .media(MimeTypeUtils.parseMimeType(finalContentType), image.getResource())
          )
          .call()
          .chatResponse();

      String analysis = response.getResult().getOutput().getText();

      ImageAnalysisResponse.TokenUsage tokenUsage = null;

      Usage usage = response.getMetadata().getUsage();
      if (usage != null) {
        tokenUsage = ImageAnalysisResponse.TokenUsage.builder()
            .promptTokens(usage.getPromptTokens())
            .completionTokens(usage.getCompletionTokens())
            .totalTokens(usage.getTotalTokens())
            .build();
      }

      return ImageAnalysisResponse.builder()
          .analysis(analysis)
          .imageType(contentType)
          .imageSize(image.getSize())
          .tokenUsage(tokenUsage)
          .build();

    } catch (Exception e) {
      log.error("Gemini 이미지 분석 처리 중 오류 발생: {}", e.getMessage());
      throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
  }

  @Transactional
  public ImageAnalysisResponse extractText(MultipartFile imageFile) throws IOException {
    String prompt = "이미지에 있는 모든 텍스트를 정확하게 추출해주세요. 텍스트만 출력하세요.";
    return analyzeImage(prompt, imageFile);
  }

}
