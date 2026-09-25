package com.sparta.msa.lesson.domain.function.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurrentTimeResponse {

  String isoFormat;
  String readableFormat;

}