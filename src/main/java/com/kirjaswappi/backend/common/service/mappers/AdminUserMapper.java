/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service.mappers;

import com.kirjaswappi.backend.common.jpa.daos.AdminUserDao;
import com.kirjaswappi.backend.common.service.entities.AdminUser;
import com.kirjaswappi.backend.common.service.enums.Role;

public final class AdminUserMapper {
  private AdminUserMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static AdminUser toEntity(AdminUserDao dao) {
    return AdminUser.builder()
        .username(dao.username())
        .password(dao.password())
        .role(Role.fromCode(dao.role()))
        .build();
  }

  public static AdminUserDao toDao(AdminUser user) {
    return AdminUserDao.builder()
        .username(user.username())
        .password(user.password())
        .role(user.role().getCode())
        .build();
  }
}
