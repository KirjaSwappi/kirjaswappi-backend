/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import lombok.*;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;

@With
@Builder
public record Book(
    String id,
    String title,
    String author,
    String description,
    Language language,
    Condition condition,
    List<Genre> genres,
    List<String> coverPhotos,
    @JsonIgnore List<MultipartFile> coverPhotoFiles,
    User owner,
    SwapCondition swapCondition,
    BookLocation location,
    Instant bookAddedAt,
    Instant bookUpdatedAt,
    Instant bookDeletedAt
) {

  public Book {
    if (bookAddedAt == null) {
      bookAddedAt = Instant.now();
    }
    if (bookUpdatedAt == null) {
      bookUpdatedAt = Instant.now();
    }
  }

  public Duration getOfferedAgo() {
    return Duration.between(bookUpdatedAt, Instant.now());
  }
}
