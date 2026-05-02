/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfanityFilterServiceTest {

  private final ProfanityFilterService service = new ProfanityFilterService();

  @Test
  @DisplayName("Returns null when input is null")
  void filterNull() {
    assertNull(service.filter(null));
  }

  @Test
  @DisplayName("Leaves clean text untouched")
  void filterCleanText() {
    String text = "I love reading books in the park.";
    assertEquals(text, service.filter(text));
  }

  @Test
  @DisplayName("Replaces a single banned word with asterisks")
  void filterReplacesSingleWord() {
    assertEquals("*** off", service.filter("fuck off"));
  }

  @Test
  @DisplayName("Replaces all occurrences of multiple banned words")
  void filterReplacesMultipleWords() {
    assertEquals("*** *** ***", service.filter("shit fuck bitch"));
  }

  @Test
  @DisplayName("Matches whole words case-insensitively")
  void filterCaseInsensitive() {
    assertEquals("*** you", service.filter("FUCK you"));
  }

  @Test
  @DisplayName("Does not strip non-word substrings (whole-word boundary)")
  void filterRespectsWordBoundary() {
    // 'classic' contains 'lass' — must NOT be considered a substring match for
    // any banned word; verify by leaving a benign sentence intact.
    String text = "We are reading a classic novel.";
    assertEquals(text, service.filter(text));
  }

  @Test
  @DisplayName("Preserves surrounding punctuation when censoring")
  void filterKeepsPunctuation() {
    assertEquals("Stop, ***!", service.filter("Stop, fuck!"));
  }
}
