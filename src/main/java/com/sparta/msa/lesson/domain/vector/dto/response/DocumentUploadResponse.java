package com.sparta.msa.lesson.domain.vector.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentUploadResponse {

  String documentId;

  String filename;

  Integer chunkCount;

}
