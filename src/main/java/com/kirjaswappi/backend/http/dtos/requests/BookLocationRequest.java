/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.BookLocation;

/**
 * Request DTO for book location data.
 */
@Getter
@Setter
public class BookLocationRequest {

  @Schema(description = "Latitude coordinate of the book location", example = "60.1699")
  private Double latitude;

  @Schema(description = "Longitude coordinate of the book location", example = "24.9384")
  private Double longitude;

  @Schema(description = "Human-readable address of the book location", example = "Mannerheimintie 12, Helsinki")
  private String address;

  @Schema(description = "City where the book is located", example = "Helsinki")
  private String city;

  @Schema(description = "Country where the book is located", example = "Finland")
  private String country;

  @Schema(description = "Postal code of the book location", example = "00100")
  private String postalCode;

  @Schema(description = "Search radius in kilometers for this book location", example = "50")
  private Integer radiusKm;

  /**
   * Converts this request DTO to a BookLocation entity.
   *
   * @return BookLocation entity
   */
  public BookLocation toEntity() {
    var location = new BookLocation();
    location.setLatitude(this.latitude);
    location.setLongitude(this.longitude);
    location.setAddress(this.address);
    location.setCity(this.city);
    location.setCountry(this.country);
    location.setPostalCode(this.postalCode);
    location.setRadiusKm(this.radiusKm != null ? this.radiusKm : 50); // Default to 50km
    return location;
  }
}
