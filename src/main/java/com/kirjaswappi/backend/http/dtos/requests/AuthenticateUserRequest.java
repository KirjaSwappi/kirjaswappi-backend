/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class AuthenticateUserRequest implements Serializable {
  @Schema(description = "The email address of the user.", example = "abc@xyz.com", requiredMode = REQUIRED)
  private String email;

  @Schema(description = "The password of the user.", example = "password", requiredMode = REQUIRED)
  private String password;

  public User toEntity() {
    this.validateProperties();
    return User.builder()
        .email(this.email.toLowerCase())
        .password(this.password)
        .build();
  }

  private void validateProperties() {
    // validate email:
    if (!ValidationUtil.validateEmail(this.email)) {
      throw new BadRequestException("invalidEmailAddress", this.email);
    }
    // validate password:
    if (!ValidationUtil.validateNotBlank(this.password)) {
      throw new BadRequestException("passwordCannotBeBlank", this.password);
    }
  }
}
