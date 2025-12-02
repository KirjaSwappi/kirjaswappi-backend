/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class UpdateBookRequest {
  @Schema(description = "The ID of the book.", example = "123456", requiredMode = REQUIRED)
  private String id;

  @Schema(description = "The title of the book.", example = "The Alchemist", requiredMode = REQUIRED)
  private String title;

  @Schema(description = "The author of the book.", example = "Paulo Coelho", requiredMode = REQUIRED)
  private String author;

  @Schema(description = "The description of the book.", example = "A novel by Paulo Coelho")
  private String description;

  @Schema(description = "The language of the book.", example = "English", requiredMode = REQUIRED)
  private String language;

  @Schema(description = "The condition of the book.", example = "New", requiredMode = REQUIRED)
  private String condition;

  @Schema(description = "The genres of the book.", example = "[\"Fiction\"]", requiredMode = REQUIRED)
  private List<String> genres;

  @Schema(description = "The cover photos of the book.", requiredMode = REQUIRED)
  private List<MultipartFile> coverPhotos;

  @Schema(description = "Swap condition of the book in JSON format.", requiredMode = REQUIRED, example = """
      {
        "conditionType": "ByBooks",
        "giveAway": false,
        "openForOffers": false,
        "genres": [],
        "books": [
          {
            "title": "The Alchemist",
            "author": "Paulo Coelho",
            "coverPhoto": "swappable-book-cover-photo.jpg"
          }
        ]
      }""")
  private String swapCondition;

  @Schema(description = "Location information for the book in JSON format.", example = """
      {
        "latitude": 60.1699,
        "longitude": 24.9384,
        "address": "Mannerheimintie 12",
        "city": "Helsinki",
        "country": "Finland",
        "postalCode": "00100",
        "radiusKm": 50
      }""")
  private String location;

  public Book toEntity() {
    this.validateProperties();
    return Book.builder()
        .id(this.id)
        .title(this.title)
        .author(this.author)
        .description(this.description)
        .language(Language.fromCode(this.language))
        .condition(Condition.fromCode(this.condition))
        .genres(this.genres.stream().map(Genre::new).toList())
        .coverPhotoFiles(this.coverPhotos)
        .build();
  }

  private void validateProperties() {
    if (!ValidationUtil.validateNotBlank(this.id)) {
      throw new BadRequestException("bookIdCannotBeBlank", this.id);
    }
    if (!ValidationUtil.validateNotBlank(this.title)) {
      throw new BadRequestException("bookTitleCannotBeBlank", this.title);
    }
    if (!ValidationUtil.validateNotBlank(this.author)) {
      throw new BadRequestException("authorCannotBeBlank", this.author);
    }
    if (!ValidationUtil.validateNotBlank(this.language)) {
      throw new BadRequestException("languageCannotBeBlank", this.language);
    }
    if (!ValidationUtil.validateNotBlank(this.condition)) {
      throw new BadRequestException("conditionCannotBeBlank", this.condition);
    }
    if (this.genres == null || this.genres.isEmpty()) {
      throw new BadRequestException("atLeastOneGenreNeeded", this.genres);
    }
    if (!ValidationUtil.validateNotBlank(this.swapCondition)) {
      throw new BadRequestException("swapConditionIsRequired");
    }
    if (this.coverPhotos == null) {
      throw new BadRequestException("atLeastOneCoverPhotoIsRequired");
    }
    for (var coverPhoto : this.coverPhotos) {
      ValidationUtil.validateMediaType(coverPhoto);
    }
  }
}
