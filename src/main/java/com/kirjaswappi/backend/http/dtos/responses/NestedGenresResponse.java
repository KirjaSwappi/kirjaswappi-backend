/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedGenresResponse {
  private Map<String, ParentGenreResponse> parentGenres;

  public NestedGenresResponse(Map<String, ParentGenreResponse> parentGenres) {
    this.parentGenres = parentGenres;
  }
}
