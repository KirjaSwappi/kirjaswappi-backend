/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Response confirming OTP verification")
public class VerifyOtpResponse {
  @Schema(description = "Confirmation message indicating OTP was verified", example = "OTP verified for user@example.com successfully.")
  private String message;

  public VerifyOtpResponse(String email) {
    this.message = "OTP verified for " + email + " successfully.";
  }
}
