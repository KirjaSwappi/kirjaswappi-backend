/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import static com.kirjaswappi.backend.common.utils.Util.hashPassword;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.mongodb.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
  private String id;
  private String firstName;
  private String lastName;
  private String email;
  private String password;
  private String streetName;
  private String houseNumber;
  private Integer zipCode;
  private String city;
  private String country;
  private String phoneNumber;
  private String aboutMe;
  private List<Genre> favGenres;
  @Nullable
  private String profilePhoto;
  @Nullable
  private String coverPhoto;
  @Nullable
  private List<Book> books;
  @Nullable
  private List<Book> favBooks;

  public void setPassword(String password, String salt) {
    this.password = hashPassword(password, salt);
  }
}
