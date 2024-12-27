/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.lang.Nullable;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDao {
  @Id
  private String id;

  @NotNull
  private String firstName;

  @NotNull
  private String lastName;

  @NotNull
  private String email;

  @NotNull
  private String password;

  @NotNull
  private String salt;

  @NotNull
  private boolean isEmailVerified;

  @Nullable
  private String streetName;

  @Nullable
  private String houseNumber;

  @Nullable
  private Integer zipCode;

  @Nullable
  private String city;

  @Nullable
  private String country;

  @Nullable
  private String phoneNumber;

  @Nullable
  private String aboutMe;

  @Nullable
  @DBRef
  private List<GenreDao> favGenres;

  @Nullable
  private String profilePhoto;

  @Nullable
  private String coverPhoto;

  @Nullable
  @DBRef
  private List<BookDao> books;
}
