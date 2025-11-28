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
public class CreateUserResponse {
  private String id;
  private String firstName;
  private String lastName;
  private String email;

  public CreateUserResponse(User entity) {
    this.id = entity.id();
    this.firstName = entity.firstName();
    this.lastName = entity.lastName();
    this.email = entity.email();
  }
}
