/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.lang.Nullable;

@Document(collection = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDao {
  @Id
  private String id;

  @NotNull
  private String title;

  @NotNull
  private String author;

  @Nullable
  private String description;

  @NotNull
  private String language;

  @NotNull
  private String condition;

  @NotNull
  private List<String> coverPhotos;

  @NotNull
  @DBRef
  private List<GenreDao> genres;

  @NotNull
  @DBRef
  private UserDao owner;

  @NotNull
  private SwapConditionDao swapCondition;

  @Nullable
  private BookLocationDao location;

  @NotNull
  private Instant bookAddedAt = Instant.now();

  @NotNull
  private Instant bookUpdatedAt = Instant.now();

  @Nullable
  private Instant bookDeletedAt;

  @NotNull
  private boolean isDeleted = false;
}
