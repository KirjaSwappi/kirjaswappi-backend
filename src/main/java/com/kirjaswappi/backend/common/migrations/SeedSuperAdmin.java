/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.migrations;

import java.util.Locale;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.kirjaswappi.backend.common.jpa.daos.AdminUserDao;
import com.kirjaswappi.backend.common.service.enums.Role;

@ChangeUnit(id = "seedSuperAdmin", order = "0006", author = "mahiuddinalkamal")
public class SeedSuperAdmin {

  private final MongoTemplate mongoTemplate;

  public SeedSuperAdmin(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Execution
  public void executeMigration() {
    String username = System.getenv("SUPER_ADMIN_USERNAME");
    String password = System.getenv("SUPER_ADMIN_PASSWORD");

    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      return;
    }

    String normalizedUsername = username.toLowerCase(Locale.ROOT);

    boolean exists = mongoTemplate.exists(
        Query.query(Criteria.where("username").is(normalizedUsername)),
        AdminUserDao.class);

    if (!exists) {
      AdminUserDao admin = AdminUserDao.builder()
          .username(normalizedUsername)
          .password(password)
          .role(Role.ADMIN.getCode())
          .build();
      mongoTemplate.save(admin);
    }
  }

  @RollbackExecution
  public void rollback() {
    String username = System.getenv("SUPER_ADMIN_USERNAME");
    if (username != null && !username.isBlank()) {
      mongoTemplate.remove(
          Query.query(Criteria.where("username").is(username.toLowerCase(Locale.ROOT))),
          AdminUserDao.class);
    }
  }
}
