/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import com.kirjaswappi.backend.jpa.daos.BookLocationDao;
import com.kirjaswappi.backend.service.entities.BookLocation;

/**
 * Mapper for converting between BookLocation entity and BookLocationDao.
 */
public final class BookLocationMapper {

  private BookLocationMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  /**
   * Converts BookLocation entity to BookLocationDao.
   *
   * @param entity the BookLocation entity
   * @return BookLocationDao or null if entity is null
   */
  public static BookLocationDao toDao(BookLocation entity) {
    if (entity == null) {
      return null;
    }

    return BookLocationDao.builder()
        .latitude(entity.latitude())
        .longitude(entity.longitude())
        .address(entity.address())
        .city(entity.city())
        .country(entity.country())
        .postalCode(entity.postalCode())
        .radiusKm(entity.radiusKm())
        .coordinates(entity.hasCoordinates() ? new Double[] { entity.longitude(), entity.latitude() } : null)
        .build();
  }

  /**
   * Converts BookLocationDao to BookLocation entity.
   *
   * @param dao the BookLocationDao
   * @return BookLocation entity or null if dao is null
   */
  public static BookLocation toEntity(BookLocationDao dao) {
    if (dao == null) {
      return null;
    }

    Double latitude = dao.latitude();
    Double longitude = dao.longitude();

    // If lat/lon are missing but GeoJSON coordinates exist, use those
    if ((latitude == null || longitude == null) && dao.coordinates() != null
        && dao.coordinates().length == 2) {
      longitude = dao.coordinates()[0];
      latitude = dao.coordinates()[1];
    }

    return BookLocation.builder()
        .latitude(latitude)
        .longitude(longitude)
        .address(dao.address())
        .city(dao.city())
        .country(dao.country())
        .postalCode(dao.postalCode())
        .radiusKm(dao.radiusKm())
        .build();
  }
}
