/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.mongodb.lang.Nullable;

/**
 * DAO for book location data stored in MongoDB. This is embedded within BookDao
 * as a subdocument. Supports both individual lat/lng fields and GeoJSON Point
 * format for accurate geospatial queries.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookLocationDao {

  /**
   * Latitude coordinate of the book location.
   */
  @Nullable
  private Double latitude;

  /**
   * Longitude coordinate of the book location.
   */
  @Nullable
  private Double longitude;

  /**
   * GeoJSON Point coordinates for accurate geospatial queries. Format:
   * [longitude, latitude] (note: longitude first in GeoJSON)
   */
  @Nullable
  private Double[] coordinates;

  /**
   * Human-readable address of the book location.
   */
  @Nullable
  private String address;

  /**
   * City where the book is located.
   */
  @Nullable
  private String city;

  /**
   * Country where the book is located.
   */
  @Nullable
  private String country;

  /**
   * Postal code of the book location.
   */
  @Nullable
  private String postalCode;

  /**
   * Search radius in kilometers for this book location. Default is 50km if not
   * specified.
   */
  @Nullable
  private Integer radiusKm = 50;
}
