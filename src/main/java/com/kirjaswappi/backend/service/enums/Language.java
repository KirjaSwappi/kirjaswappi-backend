/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.experimental.Accessors;

import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Accessors(fluent = true)
public enum Language {
  ENGLISH("English"),
  FINNISH("Finnish"),
  SWEDISH("Swedish"),
  GERMAN("German"),
  FRENCH("French"),
  SPANISH("Spanish"),
  ITALIAN("Italian"),
  RUSSIAN("Russian"),
  CHINESE("Chinese"),
  JAPANESE("Japanese"),
  KOREAN("Korean"),
  HINDI("Hindi"),
  BENGALI("Bengali"),
  ARABIC("Arabic"),
  PERSIAN("Persian"),
  TURKISH("Turkish"),
  GREEK("Greek"),
  LATIN("Latin"),
  OTHER("Other");

  private final String code;

  Language(String code) {
    this.code = code;
  }

  public static Language fromCode(String code) {
    Objects.requireNonNull(code);
    return Arrays.stream(Language.values())
        .filter(l -> l.code().equalsIgnoreCase(code))
        .findFirst()
        .orElseThrow(() -> new BadRequestException("invalidLanguage", code));
  }

  public static List<String> getSupportedLanguages() {
    return Stream.of(Language.values())
        .map(Language::code)
        .sorted()
        .collect(Collectors.toList());
  }
}
