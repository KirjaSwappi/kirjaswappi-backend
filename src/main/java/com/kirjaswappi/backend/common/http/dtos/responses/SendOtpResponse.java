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
@Schema(description = "Response confirming OTP was sent")
public class SendOtpResponse {
  @Schema(description = "Confirmation message indicating OTP was sent", example = "OTP sent to email: user@example.com")
  private String message;

  public SendOtpResponse(String email) {
    this.message = "OTP sent to email: " + email;
  }
}
