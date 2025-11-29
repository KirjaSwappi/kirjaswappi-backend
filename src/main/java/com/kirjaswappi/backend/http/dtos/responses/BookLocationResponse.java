/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.BookLocation;

/**
 * Response DTO for book location data.
 */
@Getter
@Setter
@NoArgsConstructor
public class BookLocationResponse {

  private Double latitude;
  private Double longitude;
  private String address;
  private String city;
  private String country;
  private String postalCode;
  private Integer radiusKm;

  /**
   * Constructor that creates response from BookLocation entity.
   *
   * @param entity the BookLocation entity
   */
  public BookLocationResponse(BookLocation entity) {
    if (entity != null) {
      this.latitude = entity.latitude();
      this.longitude = entity.longitude();
      this.address = entity.address();
      this.city = entity.city();
      this.country = entity.country();
      this.postalCode = entity.postalCode();
      this.radiusKm = entity.radiusKm();
    }
  }
}
