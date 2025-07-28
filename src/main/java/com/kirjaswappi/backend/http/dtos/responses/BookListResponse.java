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
  private String offeredAgo;
  private String ownerId;
  private String offeredBy;

  public BookListResponse(Book entity) {
    this.id = entity.getId();
    this.title = entity.getTitle();
    this.author = entity.getAuthor();
    this.genres = entity.getGenres().stream().map(Genre::getName).toList();
    this.language = entity.getLanguage().getCode();
    this.description = entity.getDescription();
    this.condition = entity.getCondition().getCode();
    this.coverPhotoUrl = entity.getCoverPhotos() != null ? entity.getCoverPhotos().getFirst() : null;
    this.bookLocation = entity.getOwner() != null ? entity.getOwner().getCity() : null;
    this.offeredAgo = this.getOfferedAgoHumanReadable(entity.getOfferedAgo());
    this.ownerId = entity.getOwner() != null ? entity.getOwner().getId() : null;
    this.offeredBy = entity.getOwner() != null
        ? entity.getOwner().getFirstName() + " " + entity.getOwner().getLastName()
        : null;
  }

  private String getOfferedAgoHumanReadable(Duration duration) {
    if (duration == null)
      return "";
    long seconds = duration.getSeconds();
    if (seconds < 60) {
      return seconds + " seconds ago";
    } else if (seconds < 3600) {
      long minutes = seconds / 60;
      return minutes + (minutes == 1 ? " min ago" : " mins ago");
    } else if (seconds < 86400) {
      long hours = seconds / 3600;
      return hours + (hours == 1 ? " hour ago" : " hours ago");
    } else if (seconds < 2592000) { // less than 30 days
      long days = seconds / 86400;
      return days + (days == 1 ? " day ago" : " days ago");
    } else if (seconds < 31536000) { // less than 365 days
      long months = seconds / 2592000;
      return months + (months == 1 ? " month ago" : " months ago");
    } else {
      long years = seconds / 31536000;
      return years + (years == 1 ? " year ago" : " years ago");
    }
  }
}
