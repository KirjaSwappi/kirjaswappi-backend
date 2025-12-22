/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.common.service.entities.AdminUser;

@Getter
@Setter
@Schema(description = "Response containing admin user information")
public class AdminUserResponse {
  @Schema(description = "The username of the admin user", example = "admin")
  private String username;

  @Schema(description = "The role of the admin user", example = "ADMIN")
  private String role;

  public AdminUserResponse(AdminUser adminUser) {
    this.username = adminUser.username();
    this.role = adminUser.role().getCode();
  }
}
