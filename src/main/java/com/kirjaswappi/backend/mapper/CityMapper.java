/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import com.kirjaswappi.backend.jpa.daos.CityDao;
import com.kirjaswappi.backend.service.entities.City;

public final class CityMapper {

  private CityMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static City toEntity(CityDao dao) {
    return new City(dao.id(), dao.name());
  }

  public static CityDao toDao(City entity) {
    return new CityDao(entity.getId(), entity.getName());
  }
}
