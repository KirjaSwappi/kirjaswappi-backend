/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the location information for a book. Each book can have its own
 * location, allowing users to store books in different places.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookLocation {

  /**
   * Latitude coordinate of the book location.
   */
  private Double latitude;

  /**
   * Longitude coordinate of the book location.
   */
  private Double longitude;

  /**
   * Human-readable address of the book location.
   */
  private String address;

  /**
   * City where the book is located.
   */
  private String city;

  /**
   * Country where the book is located.
   */
  private String country;

  /**
   * Postal code of the book location.
   */
  private String postalCode;

  /**
   * Search radius in kilometers for this book location. Represents how far the
   * owner is willing to travel or ship from this location. Default is 50km.
   */
  private Integer radiusKm = 50;

  /**
   * Checks if this location has valid coordinates.
   *
   * @return true if both latitude and longitude are present
   */
  public boolean hasCoordinates() {
    return latitude != null && longitude != null;
  }

  /**
   * Checks if this location has a valid address.
   *
   * @return true if address is not null and not empty
   */
  public boolean hasAddress() {
    return address != null && !address.trim().isEmpty();
  }
}
