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
    this.id = entity.getId();
    this.title = entity.getTitle();
    this.author = entity.getAuthor();
    this.genres = entity.getGenres() == null ? null : entity.getGenres().stream().map(Genre::getName).toList();
    this.language = entity.getLanguage() == null ? null : entity.getLanguage().getCode();
    this.description = entity.getDescription() == null ? null : entity.getDescription();
    this.condition = entity.getCondition() == null ? null : entity.getCondition().getCode();
    this.coverPhotoUrls = entity.getCoverPhotos() == null ? null : entity.getCoverPhotos();
    this.owner = entity.getOwner() == null ? null : new OwnerResponse(entity.getOwner());
    this.swapCondition = entity.getSwapCondition() == null ? null
        : new SwapConditionResponse(entity.getSwapCondition());
    this.location = entity.getLocation() == null ? null : new BookLocationResponse(entity.getLocation());
  }

  @Setter
  @Getter
  static class OwnerResponse {
    private String id;
    private String name;

    public OwnerResponse(User entity) {
      this.id = entity.getId();
      this.name = entity.getFirstName() + " " + entity.getLastName();
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
      this.swapType = entity.getSwapType().getCode();
      this.giveAway = entity.isGiveAway();
      this.openForOffers = entity.isOpenForOffers();
      if (entity.getSwappableGenres() != null) {
        this.swappableGenres = entity.getSwappableGenres();
      }
      if (entity.getSwappableBooks() != null) {
        this.swappableBooks = entity.getSwappableBooks()
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
