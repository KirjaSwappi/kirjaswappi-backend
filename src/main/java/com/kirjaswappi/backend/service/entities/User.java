/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import static com.kirjaswappi.backend.common.utils.Util.hashPassword;

import java.io.Serializable;
import java.util.List;

import lombok.*;
import lombok.experimental.Accessors;

import com.mongodb.lang.Nullable;

// candidate for record
@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
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

  // Todo: Should the hash generated be tied to a setter rather than setter just
  // setting the value ???
  public void setPassword(String password, String salt) {
    this.password = hashPassword(password, salt);
  }
}
