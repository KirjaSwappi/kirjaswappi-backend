/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.User;

@Getter
@Setter
public class UpdateUserResponse {
  private String id;
  private String firstName;
  private String lastName;
  private String email;
  private String streetName;
  private String houseNumber;
  private int zipCode;
  private String city;
  private String country;
  private String phoneNumber;
  private String aboutMe;
  private List<String> favGenres;

  public UpdateUserResponse(User entity) {
    this.id = entity.id();
    this.firstName = entity.firstName();
    this.lastName = entity.lastName();
    this.email = entity.email();
    this.streetName = entity.streetName();
    this.houseNumber = entity.houseNumber();
    this.zipCode = entity.zipCode();
    this.city = entity.city();
    this.country = entity.country();
    this.phoneNumber = entity.phoneNumber();
    this.aboutMe = entity.aboutMe();
    this.favGenres = entity.favGenres().stream().map(Genre::getName).toList();
  }
}
