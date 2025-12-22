/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.dtos.requests;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.common.service.entities.OTP;
import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
@Schema(description = "Request payload for verifying OTP")
public class VerifyOtpRequest {
  @Schema(description = "Email of the user.", example = "abc@xyz.com", requiredMode = REQUIRED)
  private String email;
  @Schema(description = "6 digit OTP sent to the email.", example = "123456", requiredMode = REQUIRED)
  private String otp;

  public OTP toEntity() {
    this.validateProperties();
    return new OTP(this.email.toLowerCase(), this.otp, Instant.now());
  }

  private void validateProperties() {
    if (!ValidationUtil.validateEmail(this.email)) {
      throw new BadRequestException("invalidEmailAddress", email);
    }
    if (ValidationUtil.validateOtp(this.otp)) {
      throw new BadRequestException("invalidOtp", otp);
    }
  }
}
