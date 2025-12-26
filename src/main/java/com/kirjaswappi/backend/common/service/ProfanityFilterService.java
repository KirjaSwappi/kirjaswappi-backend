/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Simple service to filter explicit or dangerous words from text. This is a
 * basic implementation that can be expanded with more comprehensive lists.
 */
@Service
public class ProfanityFilterService {

  // Placeholder list of words to filter.
  private static final Set<String> BANNED_WORDS = Set.of(
      "badword1", "badword2", "dangerousword");

  /**
   * Replaces banned words in the given text with asterisks.
   *
   * @param text The text to filter
   * @return The filtered text
   */
  public String filter(String text) {
    if (text == null) {
      return null;
    }

    String filtered = text;
    for (String word : BANNED_WORDS) {
      // Case-insensitive replacement
      filtered = filtered.replaceAll("(?i)" + Pattern.quote(word), "***");
    }
    return filtered;
  }
}
