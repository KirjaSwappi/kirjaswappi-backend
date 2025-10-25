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
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class CreateBookRequest {
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

  @Schema(description = "The user ID of the book owner.", example = "123456", requiredMode = REQUIRED)
  private String ownerId;

  @Schema(description = "Swap condition of the book in JSON format.", requiredMode = REQUIRED, example = "{\n" +
      "  \"conditionType\": \"ByBooks\",\n" +
      "  \"giveAway\": false,\n" +
      "  \"openForOffers\": false,\n" +
      "  \"genres\": [],\n" +
      "  \"books\": [\n" +
      "    {\n" +
      "      \"title\": \"The Alchemist\",\n" +
      "      \"author\": \"Paulo Coelho\",\n" +
      "      \"coverPhoto\": \"swappable-book-cover-photo.jpg\"\n" +
      "    }\n" +
      "  ]\n" +
      "}")
  private String swapCondition;

  @Schema(description = "Location information for the book")
  private BookLocationRequest location;

  public Book toEntity() {
    this.validateProperties();
    var book = new Book();
    book.setTitle(this.title);
    book.setAuthor(this.author);
    book.setDescription(this.description);
    book.setLanguage(Language.fromCode(this.language));
    book.setCondition(Condition.fromCode(this.condition));
    book.setGenres(this.genres.stream().map(Genre::new).toList());
    book.setCoverPhotoFiles(this.coverPhotos);
    var user = new User();
    user.setId(this.ownerId);
    book.setOwner(user);
    book.setLocation(this.location != null ? this.location.toEntity() : null);
    return book;
  }

  private void validateProperties() {
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
    if (!ValidationUtil.validateNotBlank(this.ownerId)) {
      throw new BadRequestException("ownerIdCannotBeBlank", this.ownerId);
    }
  }
}
