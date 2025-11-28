/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.experimental.Accessors;

import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Accessors(fluent = true)
public enum Condition {
  NEW("New"),
  LIKE_NEW("Like New"),
  GOOD("Good"),
  FAIR("Fair"),
  POOR("Poor");

  private final String code;

  Condition(String code) {
    this.code = code;
  }

  public static Condition fromCode(String code) {
    Objects.requireNonNull(code);
    return Arrays.stream(Condition.values())
        .filter(c -> c.code().equalsIgnoreCase(code))
        .findFirst()
        .orElseThrow(() -> new BadRequestException("invalidCondition", code));
  }

  public static List<String> getSupportedConditions() {
    return Stream.of(Condition.values()).map(Condition::code).toList();
  }
}
