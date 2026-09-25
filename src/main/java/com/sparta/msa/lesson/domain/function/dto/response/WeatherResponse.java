package com.sparta.msa.lesson.domain.function.dto.response;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeatherResponse {

  String city;
  int temperature;
  String condition;
  LocalDateTime timestamp;

}