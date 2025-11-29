/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.SwapCondition;
import com.kirjaswappi.backend.service.entities.SwappableBook;
import com.kirjaswappi.backend.service.entities.User;

@Getter
@Setter
public class BookResponse {
  private String id;
  private String title;
  private String author;
  private List<String> genres;
  private String language;
  private String description;
  private String condition;
  private List<String> coverPhotoUrls;
  private OwnerResponse owner;
  private SwapConditionResponse swapCondition;
  private BookLocationResponse location;

  public BookResponse(Book entity) {
    this.id = entity.id();
    this.title = entity.title();
    this.author = entity.author();
    this.genres = entity.genres() == null ? null : entity.genres().stream().map(Genre::getName).toList();
    this.language = entity.language() == null ? null : entity.language().code();
    this.description = entity.description() == null ? null : entity.description();
    this.condition = entity.condition() == null ? null : entity.condition().code();
    this.coverPhotoUrls = entity.coverPhotos() == null ? null : entity.coverPhotos();
    this.owner = entity.owner() == null ? null : new OwnerResponse(entity.owner());
    this.swapCondition = entity.swapCondition() == null ? null
        : new SwapConditionResponse(entity.swapCondition());
    this.location = entity.location() == null ? null : new BookLocationResponse(entity.location());
  }

  @Setter
  @Getter
  static class OwnerResponse {
    private String id;
    private String name;

    public OwnerResponse(User entity) {
      this.id = entity.id();
      this.name = entity.firstName() + " " + entity.lastName();
    }
  }

  @Setter
  @Getter
  static class SwapConditionResponse {
    private String swapType;
    private boolean giveAway;
    private boolean openForOffers;
    private List<Genre> swappableGenres;
    private List<SwappableBookResponse> swappableBooks;

    public SwapConditionResponse(SwapCondition entity) {
      this.swapType = entity.swapType().getCode();
      this.giveAway = entity.giveAway();
      this.openForOffers = entity.openForOffers();
      if (entity.swappableGenres() != null) {
        this.swappableGenres = entity.swappableGenres();
      }
      if (entity.swappableBooks() != null) {
        this.swappableBooks = entity.swappableBooks()
            .stream().map(SwappableBookResponse::new).toList();
      }
    }
  }

  @Setter
  @Getter
  static class SwappableBookResponse {
    private String id;
    private String title;
    private String author;
    private String coverPhotoUrl;

    public SwappableBookResponse(SwappableBook entity) {
      this.id = entity.getId();
      this.title = entity.getTitle();
      this.author = entity.getAuthor();
      this.coverPhotoUrl = entity.getCoverPhoto();
    }
  }
}
