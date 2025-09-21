/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kirjaswappi.backend.service.entities.Genre;

class NestedGenresResponseTest {

  @Test
  @DisplayName("Constructor creates NestedGenresResponse with populated parent genres map")
  void constructor_createsResponseWithParentGenres() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    Genre childGenre = new Genre("2", "Science Fiction", parentGenre);

    ParentGenreResponse parentResponse = new ParentGenreResponse(parentGenre, List.of(childGenre));
    Map<String, ParentGenreResponse> parentGenres = new HashMap<>();
    parentGenres.put("Fiction", parentResponse);

    // Act
    NestedGenresResponse response = new NestedGenresResponse(parentGenres);

    // Assert
    assertNotNull(response.getParentGenres());
    assertEquals(1, response.getParentGenres().size());
    assertTrue(response.getParentGenres().containsKey("Fiction"));
    assertEquals("1", response.getParentGenres().get("Fiction").getId());
    assertEquals("Fiction", response.getParentGenres().get("Fiction").getName());
    assertEquals(1, response.getParentGenres().get("Fiction").getChildGenres().size());
  }

  @Test
  @DisplayName("Constructor handles empty parent genres map")
  void constructor_handlesEmptyParentGenresMap() {
    // Arrange
    Map<String, ParentGenreResponse> emptyMap = new HashMap<>();

    // Act
    NestedGenresResponse response = new NestedGenresResponse(emptyMap);

    // Assert
    assertNotNull(response.getParentGenres());
    assertTrue(response.getParentGenres().isEmpty());
    assertEquals(0, response.getParentGenres().size());
  }

  @Test
  @DisplayName("Constructor handles null parent genres map")
  void constructor_handlesNullParentGenresMap() {
    // Act
    NestedGenresResponse response = new NestedGenresResponse(null);

    // Assert
    assertNull(response.getParentGenres());
  }

  @Test
  @DisplayName("Constructor creates response with multiple parent genres")
  void constructor_handlesMultipleParentGenres() {
    // Arrange
    Genre fictionParent = new Genre("1", "Fiction", null);
    Genre sciFiChild = new Genre("2", "Science Fiction", fictionParent);
    Genre fantasyChild = new Genre("3", "Fantasy", fictionParent);

    Genre nonFictionParent = new Genre("4", "Non-Fiction", null);
    Genre biographyChild = new Genre("5", "Biography", nonFictionParent);

    ParentGenreResponse fictionResponse = new ParentGenreResponse(fictionParent, List.of(sciFiChild, fantasyChild));
    ParentGenreResponse nonFictionResponse = new ParentGenreResponse(nonFictionParent, List.of(biographyChild));

    Map<String, ParentGenreResponse> parentGenres = new HashMap<>();
    parentGenres.put("Fiction", fictionResponse);
    parentGenres.put("Non-Fiction", nonFictionResponse);

    // Act
    NestedGenresResponse response = new NestedGenresResponse(parentGenres);

    // Assert
    assertNotNull(response.getParentGenres());
    assertEquals(2, response.getParentGenres().size());

    // Verify Fiction parent
    assertTrue(response.getParentGenres().containsKey("Fiction"));
    ParentGenreResponse fiction = response.getParentGenres().get("Fiction");
    assertEquals("1", fiction.getId());
    assertEquals("Fiction", fiction.getName());
    assertEquals(2, fiction.getChildGenres().size());

    // Verify Non-Fiction parent
    assertTrue(response.getParentGenres().containsKey("Non-Fiction"));
    ParentGenreResponse nonFiction = response.getParentGenres().get("Non-Fiction");
    assertEquals("4", nonFiction.getId());
    assertEquals("Non-Fiction", nonFiction.getName());
    assertEquals(1, nonFiction.getChildGenres().size());
  }

  @Test
  @DisplayName("Constructor creates response with parent genres having no children")
  void constructor_handlesParentGenresWithNoChildren() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    ParentGenreResponse parentResponse = new ParentGenreResponse(parentGenre, List.of());

    Map<String, ParentGenreResponse> parentGenres = new HashMap<>();
    parentGenres.put("Fiction", parentResponse);

    // Act
    NestedGenresResponse response = new NestedGenresResponse(parentGenres);

    // Assert
    assertNotNull(response.getParentGenres());
    assertEquals(1, response.getParentGenres().size());
    ParentGenreResponse fiction = response.getParentGenres().get("Fiction");
    assertEquals("1", fiction.getId());
    assertEquals("Fiction", fiction.getName());
    assertNotNull(fiction.getChildGenres());
    assertTrue(fiction.getChildGenres().isEmpty());
  }

  @Test
  @DisplayName("Setter and getter work correctly for parentGenres field")
  void setterAndGetter_workCorrectlyForParentGenres() {
    // Arrange
    NestedGenresResponse response = new NestedGenresResponse(null);
    Genre parentGenre = new Genre("1", "Fiction", null);
    ParentGenreResponse parentResponse = new ParentGenreResponse(parentGenre, List.of());

    Map<String, ParentGenreResponse> parentGenres = new HashMap<>();
    parentGenres.put("Fiction", parentResponse);

    // Act
    response.setParentGenres(parentGenres);

    // Assert
    assertNotNull(response.getParentGenres());
    assertEquals(parentGenres, response.getParentGenres());
    assertEquals(1, response.getParentGenres().size());
    assertTrue(response.getParentGenres().containsKey("Fiction"));
  }
}
