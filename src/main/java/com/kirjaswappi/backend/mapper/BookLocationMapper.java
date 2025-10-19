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
public class BookLocationMapper {

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

    var dao = new BookLocationDao();
    dao.setLatitude(entity.getLatitude());
    dao.setLongitude(entity.getLongitude());
    dao.setAddress(entity.getAddress());
    dao.setCity(entity.getCity());
    dao.setCountry(entity.getCountry());
    dao.setPostalCode(entity.getPostalCode());
    dao.setRadiusKm(entity.getRadiusKm());

    return dao;
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

    var entity = new BookLocation();
    entity.setLatitude(dao.getLatitude());
    entity.setLongitude(dao.getLongitude());
    entity.setAddress(dao.getAddress());
    entity.setCity(dao.getCity());
    entity.setCountry(dao.getCountry());
    entity.setPostalCode(dao.getPostalCode());
    entity.setRadiusKm(dao.getRadiusKm());

    return entity;
  }
}
