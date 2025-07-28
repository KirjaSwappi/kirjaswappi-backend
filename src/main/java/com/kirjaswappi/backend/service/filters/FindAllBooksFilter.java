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
  @Schema(description = "Optional filter parameter for the owner's user ID.", example = "64e8b2f2c2a4e2a1b8d7c9e0")
  String userId;

  @Schema(description = "Filter parameter for owner's book.", example = "64e8b2f2c2a4e2a1b8d7c9e0")
  String ownerId;

  @Schema(description = "Filter parameter for books except mine.", example = "64e8b2f2c2a4e2a1b8d7c9e0")
  String notOwnerId;

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

    var finalCriteria = new Criteria();
    return finalCriteria.andOperator(combinedCriteria.toArray(new Criteria[0]));
  }
}
