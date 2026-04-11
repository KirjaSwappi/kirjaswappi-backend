/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.User;

@Getter
@Setter
public class UserLoginResponse extends UserResponse {
  private String userToken;
  private String userRefreshToken;

  public UserLoginResponse(User entity, String userToken, String userRefreshToken) {
    super(entity);
    this.userToken = userToken;
    this.userRefreshToken = userRefreshToken;
  }
}
