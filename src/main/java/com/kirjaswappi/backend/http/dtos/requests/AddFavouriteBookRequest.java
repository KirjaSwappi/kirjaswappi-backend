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

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class AddFavouriteBookRequest {
  @Schema(description = "The ID of the favourite book.", example = "123456", requiredMode = REQUIRED)
  private String bookId;

  @Schema(description = "The user ID.", example = "123456", requiredMode = REQUIRED)
  private String userId;

  public User toEntity() {
    this.validateProperties();
    var book = Book.builder().id(bookId).build();

    return User.builder()
        .id(this.userId)
        .favBooks(List.of(book))
        .build();
  }

  private void validateProperties() {
    if (!ValidationUtil.validateNotBlank(this.bookId)) {
      throw new BadRequestException("bookIdCannotBeBlank", this.bookId);
    }
    if (!ValidationUtil.validateNotBlank(this.userId)) {
      throw new BadRequestException("userIdCannotBeBlank", this.userId);
    }
  }
}
