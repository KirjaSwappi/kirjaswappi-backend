/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.filters;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;

@Getter
@Setter
public class FindAllBooksFilter {
  @Schema(description = "Search parameter to find specific books by name, author, or genre.", example = "Lord of the Rings")
  String search;

  @Schema(description = "Filter parameter for the language of the book.", example = "[\"English\"]", allowableValues = {
      "English", "Finnish", "Bengali", "Spanish", "French", "German", "Russian", "Arabic", "Chinese", "Japanese" })
  List<String> languages;

  @Schema(description = "Filter parameter for the condition of the book.", example = "[\"New\"]", allowableValues = {
      "New", "Like New", "Good", "Fair", "Poor" })
  List<String> conditions;

  @Schema(description = "Filter parameter for the genre of the book.", example = "[\"Fiction\"]", allowableValues = {
      "Fantasy", "Science Fiction", "Mystery", "Horror", "Romance", "Thriller", "Historical Fiction", "Non-Fiction" })
  List<String> genres;

  @Schema(description = "Filter parameter for owner's book.", example = "64e8b2f2c2a4e2a1b8d7c9e0")
  String ownerId;

  @Schema(description = "Filter parameter for books except mine.", example = "64e8b2f2c2a4e2a1b8d7c9e0")
  String notOwnerId;

  @Schema(description = "Filter books within radius of these coordinates - latitude", example = "60.1699")
  Double nearLatitude;

  @Schema(description = "Filter books within radius of these coordinates - longitude", example = "24.9384")
  Double nearLongitude;

  @Schema(description = "Search radius in kilometers for location-based filtering", example = "50")
  Integer radiusKm;

  @Schema(description = "Filter books by city", example = "Helsinki")
  String city;

  @Schema(description = "Filter books by country", example = "Finland")
  String country;

  @Schema(description = "Sort field for ordering results. Use with Pageable's 'sort' parameter.", example = "createdAt", allowableValues = {
      "title", "author", "createdAt", "condition", "language" })
  String sortBy;

  @Schema(description = "Sort direction for ordering results. Use with Pageable's 'sort' parameter.", example = "desc", allowableValues = {
      "asc", "desc" })
  String sortDirection;

  public Criteria buildSearchAndFilterCriteria() {
    List<Criteria> combinedCriteria = new ArrayList<>();

    // Add search criteria:
    if (search != null && !search.isEmpty()) {
      combinedCriteria.add(new Criteria().orOperator(
          Criteria.where("title").regex(search, "i"),
          Criteria.where("author").regex(search, "i"),
          Criteria.where("description").regex(search, "i")));
    }

    // Add filter criteria:
    combinedCriteria.add(Criteria.where("isDeleted").is(false));

    // Filter by owner ID if provided:
    if (this.ownerId != null && !this.ownerId.isEmpty()) {
      combinedCriteria.add(Criteria.where("owner.$id").is(new ObjectId(this.ownerId)));
    }

    // Filter by not owner ID if provided:
    if (this.notOwnerId != null && !this.notOwnerId.isEmpty()) {
      combinedCriteria.add(Criteria.where("owner.$id").ne(new ObjectId(this.notOwnerId)));
    }

    // Add language, condition, and genre filters:
    if (languages != null && !languages.isEmpty()) {
      if (languages.size() == 1) {
        combinedCriteria.add(Criteria.where("language").is(languages.getFirst()));
      } else {
        combinedCriteria.add(new Criteria().orOperator(
            languages.stream().map(lang -> Criteria.where("language").is(lang)).toArray(Criteria[]::new)));
      }
    }

    if (conditions != null && !conditions.isEmpty()) {
      if (conditions.size() == 1) {
        combinedCriteria.add(Criteria.where("condition").is(conditions.getFirst()));
      } else {
        combinedCriteria.add(new Criteria().orOperator(
            conditions.stream().map(cond -> Criteria.where("condition").is(cond)).toArray(Criteria[]::new)));
      }
    }

    if (genres != null && !genres.isEmpty()) {
      if (genres.size() == 1) {
        combinedCriteria.add(Criteria.where("genres.name").is(genres.getFirst()));
      } else {
        combinedCriteria.add(new Criteria().orOperator(
            genres.stream().map(genre -> Criteria.where("genres.name").is(genre)).toArray(Criteria[]::new)));
      }
    }

    // Add location-based filtering with proper geospatial queries:
    if (nearLatitude != null && nearLongitude != null) {
      // Validate coordinates before using them
      if (!com.kirjaswappi.backend.service.entities.BookLocation.isValidLatitude(nearLatitude) ||
          !com.kirjaswappi.backend.service.entities.BookLocation.isValidLongitude(nearLongitude)) {
        throw new IllegalArgumentException("Invalid coordinates for location search");
      }

      int searchRadius = radiusKm != null ? radiusKm : 50;

      try {
        // Try to use MongoDB's $geoWithin with $centerSphere for accurate distance
        // calculations
        // This works with both GeoJSON coordinates and lat/lng fields
        org.springframework.data.geo.Point center = new org.springframework.data.geo.Point(nearLongitude, nearLatitude);
        org.springframework.data.geo.Distance distance = new org.springframework.data.geo.Distance(searchRadius,
            org.springframework.data.geo.Metrics.KILOMETERS);
        org.springframework.data.geo.Circle circle = new org.springframework.data.geo.Circle(center, distance);

        // Use $geoWithin for accurate spherical distance calculation
        combinedCriteria.add(Criteria.where("location.coordinates").withinSphere(circle));
      } catch (Exception e) {
        // Fallback to bounding box approach if geospatial query fails
        // This provides approximate results but avoids the worst inaccuracies near
        // poles
        double latDelta = Math.min(searchRadius / 111.0, 10.0); // Cap at 10 degrees
        double lngDelta = Math.min(searchRadius / (111.0 * Math.cos(Math.toRadians(Math.abs(nearLatitude)))), 20.0); // Cap
                                                                                                                     // at
                                                                                                                     // 20
                                                                                                                     // degrees

        combinedCriteria.add(Criteria.where("location.latitude")
            .gte(Math.max(nearLatitude - latDelta, -85.0))
            .lte(Math.min(nearLatitude + latDelta, 85.0)));
        combinedCriteria.add(Criteria.where("location.longitude")
            .gte(Math.max(nearLongitude - lngDelta, -180.0))
            .lte(Math.min(nearLongitude + lngDelta, 180.0)));
      }
    }

    // Filter by city if provided:
    if (city != null && !city.isEmpty()) {
      combinedCriteria.add(Criteria.where("location.city").regex(city, "i"));
    }

    // Filter by country if provided:
    if (country != null && !country.isEmpty()) {
      combinedCriteria.add(Criteria.where("location.country").regex(country, "i"));
    }

    var finalCriteria = new Criteria();
    return finalCriteria.andOperator(combinedCriteria.toArray(new Criteria[0]));
  }
}
