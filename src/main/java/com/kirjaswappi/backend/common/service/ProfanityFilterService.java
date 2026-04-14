/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ProfanityFilterService {

  private static final Set<String> BANNED_WORDS = Set.of(
      "fuck", "shit", "asshole", "bitch", "bastard",
      "dick", "cunt", "damn", "piss", "slut",
      "whore", "nigger", "faggot", "retard", "twat",
      "wanker", "bollocks", "motherfucker", "cocksucker", "arse");

  private static final List<Pattern> BANNED_PATTERNS;

  static {
    BANNED_PATTERNS = BANNED_WORDS.stream()
        .map(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE))
        .toList();
  }

  public String filter(String text) {
    if (text == null) {
      return null;
    }

    String filtered = text;
    for (Pattern pattern : BANNED_PATTERNS) {
      filtered = pattern.matcher(filtered).replaceAll("***");
    }
    return filtered;
  }
}
