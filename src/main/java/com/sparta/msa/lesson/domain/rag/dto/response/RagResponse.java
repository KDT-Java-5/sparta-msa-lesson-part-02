package com.sparta.msa.lesson.domain.rag.dto.response;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagResponse {

  String answer;

  List<DocumentSource> sources;

  @Getter
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class DocumentSource {

    String filename;

    String documentId;

    String preview;

  }
}