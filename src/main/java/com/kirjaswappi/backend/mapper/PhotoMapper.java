/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import com.kirjaswappi.backend.jpa.daos.PhotoDao;
import com.kirjaswappi.backend.service.entities.Photo;

public final class PhotoMapper {
  private PhotoMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static Photo toEntity(String id, String imageUrl) {
    return Photo.builder()
        .id(id)
        .coverPhoto(imageUrl)
        .build();
  }

  public static PhotoDao toDao(Photo entity) {
    return PhotoDao.builder()
        .id(entity.id())
        .coverPhoto(entity.coverPhoto())
        .build();
  }
}
