/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.dtos.requests;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
@Schema(description = "Request payload for sending OTP to user email")
public class SendOtpRequest {
  @Schema(description = "The email address of the user.", example = "abc@xyz.com", requiredMode = REQUIRED)
  private String email;

  public String toEntity() {
    this.validateProperties();
    return this.email.toLowerCase();
  }

  private void validateProperties() {
    if (!ValidationUtil.validateEmail(this.email)) {
      throw new BadRequestException("invalidEmailAddress", this.email);
    }
  }
}
