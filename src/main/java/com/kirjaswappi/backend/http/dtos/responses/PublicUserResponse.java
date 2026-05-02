/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.User;

/**
 * Minimal user profile fields safe to return to other authenticated users.
 * Intentionally excludes email, address, phone, and favourite books.
 */
@Getter
@Setter
public class PublicUserResponse {
  private String id;
  private String firstName;
  private String lastName;
  private String city;
  private String country;
  private String aboutMe;
  private String profilePhoto;
  private List<String> favGenres;
  private List<BookListResponse> books;

  public PublicUserResponse(User entity) {
    this.id = entity.id();
    this.firstName = entity.firstName();
    this.lastName = entity.lastName();
    this.city = entity.city();
    this.country = entity.country();
    this.aboutMe = entity.aboutMe();
    this.profilePhoto = entity.profilePhoto();
    this.favGenres = entity.favGenres() != null ? entity.favGenres().stream().map(Genre::getName).toList()
        : List.of();
    this.books = entity.books() != null ? entity.books().stream().map(BookListResponse::new).toList()
        : List.of();
  }
}
