/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.dtos.responses;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.common.service.entities.AdminUser;

@Getter
@Setter
public class AdminUserResponse {
  private String username;
  private String role;

  public AdminUserResponse(AdminUser adminUser) {
    this.username = adminUser.username();
    this.role = adminUser.role().getCode();
  }
}
