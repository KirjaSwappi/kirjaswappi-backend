/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordResponse {
  private String message;

  public ChangePasswordResponse(String email) {
    this.message = "Password changed for user: " + email;
  }
}
