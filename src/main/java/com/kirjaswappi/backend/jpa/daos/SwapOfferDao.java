/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import lombok.*;
import lombok.experimental.Accessors;

import org.springframework.data.mongodb.core.mapping.DBRef;

import com.mongodb.lang.Nullable;

@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class SwapOfferDao {
  @Nullable
  @DBRef
  private SwappableBookDao offeredBook;

  @Nullable
  @DBRef
  private GenreDao offeredGenre;
}
