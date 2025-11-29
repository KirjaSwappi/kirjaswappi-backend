/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class SwapConditionDao {
  @NotNull
  private String swapType;

  @NotNull
  private boolean giveAway;

  @NotNull
  private boolean openForOffers;

  @NotNull
  private List<GenreDao> swappableGenres;

  @NotNull
  private List<SwappableBookDao> swappableBooks;
}
