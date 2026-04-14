/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class ResetPasswordRequest {
  @Schema(description = "The new password of the user.", example = "newPassword", requiredMode = REQUIRED)
  private String newPassword;

  @Schema(description = "The confirm password of the user.", example = "newPassword", requiredMode = REQUIRED)
  private String confirmPassword;

  @Schema(description = "The reset token obtained from OTP verification.", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = REQUIRED)
  private String resetToken;

  public User toUserEntity(String email) {

    this.validateProperties(email);

    return User.builder()
        .email(email.toLowerCase())
        .password(this.newPassword)
        .build();
  }

  private void validateProperties(String email) {
    // validate email:
    if (!ValidationUtil.validateEmail(email)) {
      throw new BadRequestException("invalidEmailAddress", email);
    }
    // validate reset token:
    if (!ValidationUtil.validateNotBlank(this.resetToken)) {
      throw new BadRequestException("resetTokenRequired", email);
    }
    // validate new password:
    ValidationUtil.validatePassword(this.newPassword, "newPassword");
    // validate confirm password:
    if (!ValidationUtil.validateNotBlank(this.confirmPassword)) {
      throw new BadRequestException("confirmPasswordCannotBeBlank", this.confirmPassword);
    }
    // validate new password and confirm password matches:
    if (!this.newPassword.equals(this.confirmPassword)) {
      throw new BadRequestException("passwordsDoNotMatch", this.confirmPassword);
    }
  }
}
