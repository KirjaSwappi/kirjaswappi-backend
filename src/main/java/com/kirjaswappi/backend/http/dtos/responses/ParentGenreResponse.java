/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.Genre;

@Getter
@Setter
public class ParentGenreResponse {
  private String id;
  private String name;
  private List<ChildGenreResponse> childGenres;

  public ParentGenreResponse(Genre entity, List<Genre> children) {
    this.id = entity.getId();
    this.name = entity.getName();
    this.childGenres = children.stream().map(ChildGenreResponse::new).toList();
  }
}
