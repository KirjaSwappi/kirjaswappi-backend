/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.jpa.repositories;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.kirjaswappi.backend.common.jpa.daos.AdminUserDao;

public interface AdminUserRepository extends MongoRepository<@NotNull AdminUserDao, @NotNull String> {

  Optional<AdminUserDao> findByUsername(String username);

  void deleteByUsername(String username);
}
