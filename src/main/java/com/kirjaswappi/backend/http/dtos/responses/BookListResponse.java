/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.time.Duration;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.hateoas.server.core.Relation;

import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;

@Getter
@Setter
@Relation(collectionRelation = "books")
public class BookListResponse {
  private String id;
  private String title;
  private String author;
  private List<String> genres;
  private String language;
  private String description;
  private String condition;
  private String coverPhotoUrl;
  private String bookLocation;
  private BookLocationResponse location;
  private String offeredAgo;
  private String ownerId;
  private String offeredBy;

  public BookListResponse(Book entity) {
    this.id = entity.id();
    this.title = entity.title();
    this.author = entity.author();
    this.genres = entity.genres().stream().map(Genre::getName).toList();
    this.language = entity.language().code();
    this.description = entity.description();
    this.condition = entity.condition().code();
    this.coverPhotoUrl = entity.coverPhotos() != null && !entity.coverPhotos().isEmpty()
        ? entity.coverPhotos().getFirst()
        : null;
    this.bookLocation = entity.owner() != null ? entity.owner().city() : null;
    this.location = entity.location() != null ? new BookLocationResponse(entity.location()) : null;
    this.offeredAgo = this.getOfferedAgoHumanReadable(entity.getOfferedAgo());
    this.ownerId = entity.owner() != null ? entity.owner().id() : null;
    this.offeredBy = entity.owner() != null
        ? entity.owner().firstName() + " " + entity.owner().lastName()
        : null;
  }

  private String getOfferedAgoHumanReadable(Duration duration) {

    if (duration == null) {
      return "";
    }

    return switch (Long.valueOf(duration.getSeconds())) {

    case Long seconds when seconds < 60 -> seconds + " seconds ago";

    case Long seconds when seconds < 3600 -> {
      long minutes = seconds / 60;
      yield minutes + (minutes == 1 ? " min ago" : " mins ago");
    }

    case Long seconds when seconds < 86400 -> {
      long hours = seconds / 3600;
      yield hours + (hours == 1 ? " hour ago" : " hours ago");
    }

    case Long seconds when seconds < 2592000 -> {
      long days = seconds / 86400;
      yield days + (days == 1 ? " day ago" : " days ago");
    }

    case Long seconds when seconds < 31536000 -> {
      long months = seconds / 2592000;
      yield months + (months == 1 ? " month ago" : " months ago");
    }
    default -> {
      long years = duration.getSeconds() / 31536000;
      yield years + (years == 1 ? " year ago" : " years ago");
    }
    };
  }

}
