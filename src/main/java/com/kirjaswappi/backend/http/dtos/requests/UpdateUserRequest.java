/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class UpdateUserRequest {
  @Schema(description = "The id of the user.", example = "123456", requiredMode = REQUIRED)
  private String id;

  @Schema(description = "The first name of the user.", example = "Robert", requiredMode = REQUIRED)
  private String firstName;

  @Schema(description = "The last name of the user.", example = "Smith", requiredMode = REQUIRED)
  private String lastName;

  @Schema(description = "The street name of the user.", example = "Street 1", requiredMode = NOT_REQUIRED)
  private String streetName;

  @Schema(description = "The house number of the user.", example = "1", requiredMode = NOT_REQUIRED)
  private String houseNumber;

  @Schema(description = "The zip code of the user.", example = "12345", requiredMode = NOT_REQUIRED)
  private Integer zipCode;

  @Schema(description = "The city of the user.", example = "Dhaka", requiredMode = NOT_REQUIRED)
  private String city;

  @Schema(description = "The country of the user.", example = "Bangladesh", requiredMode = NOT_REQUIRED)
  private String country;

  @Schema(description = "The phone number of the user.", example = "1234567890", requiredMode = NOT_REQUIRED)
  private String phoneNumber;

  @Schema(description = "The about me of the user.", example = "I am a software engineer.", requiredMode = NOT_REQUIRED)
  private String aboutMe;

  @Schema(description = "The favorite genres of the user.", example = "[\"Fiction\", \"Non-fiction\"]", requiredMode = NOT_REQUIRED)
  private List<String> favGenres;

  public User toEntity() {
    this.validateProperties();
    return User.builder()
        .id(this.id)
        .firstName(this.firstName)
        .lastName(this.lastName)
        .streetName(this.streetName)
        .houseNumber(this.houseNumber)
        .zipCode(this.zipCode)
        .city(this.city)
        .country(this.country)
        .phoneNumber(this.phoneNumber)
        .aboutMe(this.aboutMe)
        .favGenres(this.favGenres == null ? null : this.favGenres.stream().map(Genre::new).toList())
        .build();
  }

  private void validateProperties() {
    // validate id:
    if (!ValidationUtil.validateNotBlank(this.id)) {
      throw new BadRequestException("idCannotBeBlank", this.id);
    }
    // validate first name:
    if (!ValidationUtil.validateNotBlank(this.firstName)) {
      throw new BadRequestException("firstNameCannotBeBlank", this.firstName);
    }
    // validate last name:
    if (!ValidationUtil.validateNotBlank(this.lastName)) {
      throw new BadRequestException("lastNameCannotBeBlank", this.lastName);
    }
  }
}
