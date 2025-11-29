/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import java.util.Objects;
import java.util.UUID;

import com.kirjaswappi.backend.jpa.daos.SwappableBookDao;
import com.kirjaswappi.backend.service.entities.SwappableBook;

public final class SwappableBookMapper {

  private SwappableBookMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static SwappableBook toEntity(SwappableBookDao dao) {
    return SwappableBook.builder()
        .id(dao.id())
        .title(dao.title())
        .author(dao.author())
        .coverPhoto(dao.coverPhoto())
        .isDeleted(dao.isDeleted())
        .build();
  }

  public static SwappableBook toEntity(SwappableBookDao dao, String imageUrl) {
    Objects.requireNonNull(imageUrl);

    return SwappableBook.builder()
        .id(dao.id())
        .title(dao.title())
        .author(dao.author())
        .coverPhoto(imageUrl)
        .isDeleted(dao.isDeleted())
        .build();
  }

  public static SwappableBookDao toDao(SwappableBook entity) {
    return SwappableBookDao.builder()
        .id(entity.getId() != null ? entity.getId() : UUID.randomUUID().toString())
        .title(entity.getTitle())
        .author(entity.getAuthor())
        .coverPhoto(entity.getCoverPhoto() != null ? entity.getCoverPhoto() : null)
        .isDeleted(entity.isDeleted())
        .build();
  }

  public static SwappableBookDao toDao(SwappableBookDao dao, String imageUrl) {
    Objects.requireNonNull(dao.id());
    Objects.requireNonNull(imageUrl);
    dao.coverPhoto(imageUrl);
    return dao;
  }
}
