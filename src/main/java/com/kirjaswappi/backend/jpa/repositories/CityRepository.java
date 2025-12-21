/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.kirjaswappi.backend.jpa.daos.CityDao;

public interface CityRepository extends MongoRepository<CityDao, String> {
  boolean existsByName(String name);

  Optional<CityDao> findByName(String name);
}
