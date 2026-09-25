package com.sparta.msa.lesson.domain.vector.dto.request;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimilaritySearchRequest {

  UUID documentId;

  String query;

  Integer topK;

}
