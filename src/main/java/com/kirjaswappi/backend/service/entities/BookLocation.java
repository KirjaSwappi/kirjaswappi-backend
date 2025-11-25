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
   * @return true if both latitude and longitude are present and within valid
   *         ranges
   */
  public boolean hasCoordinates() {
    return latitude != null && longitude != null && isValidLatitude(latitude) && isValidLongitude(longitude);
  }

  /**
   * Checks if this location has a valid address.
   *
   * @return true if address is not null and not empty
   */
  public boolean hasAddress() {
    return address != null && !address.trim().isEmpty();
  }

  /**
   * Validates if the latitude is within acceptable range. Restricts to -85 to 85
   * degrees to avoid issues near poles where longitude degrees converge.
   *
   * @param lat the latitude to validate
   * @return true if latitude is valid
   */
  public static boolean isValidLatitude(Double lat) {
    return lat != null && lat >= -85.0 && lat <= 85.0;
  }

  /**
   * Validates if the longitude is within acceptable range.
   *
   * @param lng the longitude to validate
   * @return true if longitude is valid
   */
  public static boolean isValidLongitude(Double lng) {
    return lng != null && lng >= -180.0 && lng <= 180.0;
  }
}
