package com.sparta.msa.lesson.domain.ai.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageAnalysisRequest {

  String message;

  MultipartFile image;

}
