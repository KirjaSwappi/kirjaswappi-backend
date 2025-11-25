/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.BookLocation;

/**
 * Response DTO for book location data.
 */
@Getter
@Setter
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
      this.latitude = entity.getLatitude();
      this.longitude = entity.getLongitude();
      this.address = entity.getAddress();
      this.city = entity.getCity();
      this.country = entity.getCountry();
      this.postalCode = entity.getPostalCode();
      this.radiusKm = entity.getRadiusKm();
    }
  }

  /**
   * Default constructor for serialization.
   */
  public BookLocationResponse() {
  }
}
