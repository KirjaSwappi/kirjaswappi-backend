/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NestedGenresResponse implements Serializable {
  private Map<String, ParentGenreResponse> parentGenres;

  public NestedGenresResponse(Map<String, ParentGenreResponse> parentGenres) {
    this.parentGenres = parentGenres;
  }
}
