/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import com.kirjaswappi.backend.jpa.daos.GenreDao;
import com.kirjaswappi.backend.service.entities.Genre;

public final class GenreMapper {

  private GenreMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static Genre toEntity(GenreDao dao) {
    return new Genre(dao.id(), dao.name(),
        dao.parent() == null ? null : toEntity(dao.parent()));
  }

  public static GenreDao toDao(Genre entity) {
    return new GenreDao(entity.getId(), entity.getName(),
        entity.getParent() == null ? null : toDao(entity.getParent()));
  }
}
