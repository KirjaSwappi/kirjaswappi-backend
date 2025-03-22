/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dto.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.http.validation.ValidationUtil;
import com.kirjaswappi.backend.service.entity.User;
import com.kirjaswappi.backend.service.exception.BadRequestException;

@Getter
@Setter
public class CreateUserRequest {
  @Schema(description = "The first name of the user.", example = "Robert", requiredMode = REQUIRED)
  private String firstName;

  @Schema(description = "The last name of the user.", example = "Smith", requiredMode = REQUIRED)
  private String lastName;

  @Schema(description = "The email address of the user.", example = "abc@xyz.com", requiredMode = REQUIRED)
  private String email;

  @Schema(description = "The password of the user.", example = "password", requiredMode = REQUIRED)
  private String password;

  @Schema(description = "The confirm password of the user.", example = "password", requiredMode = REQUIRED)
  private String confirmPassword;

  public User toEntity() {
    this.validateProperties();
    var entity = new User();
    entity.setFirstName(this.firstName);
    entity.setLastName(this.lastName);
    entity.setEmail(this.email.toLowerCase());
    entity.setPassword(this.password);
    return entity;
  }

  private void validateProperties() {
    // validate first name:
    if (!ValidationUtil.validateNotBlank(this.firstName)) {
      throw new BadRequestException("firstNameCannotBeBlank", this.firstName);
    }
    // validate last name:
    if (!ValidationUtil.validateNotBlank(this.lastName)) {
      throw new BadRequestException("lastNameCannotBeBlank", this.lastName);
    }
    // validate email:
    if (!ValidationUtil.validateEmail(this.email)) {
      throw new BadRequestException("invalidEmailAddress", this.email);
    }
    // validate password:
    if (!ValidationUtil.validateNotBlank(this.password)) {
      throw new BadRequestException("passwordCannotBeBlank", this.password);
    }
    // validate confirm password:
    if (!ValidationUtil.validateNotBlank(this.confirmPassword)) {
      throw new BadRequestException("confirmPasswordCannotBeBlank", this.confirmPassword);
    }
    // validate password and confirm password:
    if (!this.password.equals(this.confirmPassword)) {
      throw new BadRequestException("passwordsDoNotMatch", this.confirmPassword);
    }
  }
}
