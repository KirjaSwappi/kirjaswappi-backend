/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kirjaswappi.backend.service.entities.Genre;

class ChildGenreResponseTest {

  @Test
  @DisplayName("Constructor creates ChildGenreResponse with Genre entity fields")
  void constructor_createsResponseWithGenreEntityFields() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    Genre childGenre = new Genre("2", "Science Fiction", parentGenre);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertEquals("2", response.getId());
    assertEquals("Science Fiction", response.getName());
  }

  @Test
  @DisplayName("Constructor properly maps Genre entity id and name fields")
  void constructor_mapsGenreEntityFieldsCorrectly() {
    // Arrange
    Genre parentGenre = new Genre("parent-123", "Fiction", null);
    Genre childGenre = new Genre("child-456", "Fantasy Fiction", parentGenre);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertEquals("child-456", response.getId());
    assertEquals("Fantasy Fiction", response.getName());
  }

  @Test
  @DisplayName("Constructor handles Genre entity with null id")
  void constructor_handlesNullId() {
    // Arrange
    Genre childGenre = new Genre(null, "Science Fiction", null);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertNull(response.getId());
    assertEquals("Science Fiction", response.getName());
  }

  @Test
  @DisplayName("Constructor handles Genre entity with null name")
  void constructor_handlesNullName() {
    // Arrange
    Genre childGenre = new Genre("123", null, null);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertEquals("123", response.getId());
    assertNull(response.getName());
  }

  @Test
  @DisplayName("Constructor handles Genre entity with both null id and name")
  void constructor_handlesBothNullIdAndName() {
    // Arrange
    Genre childGenre = new Genre(null, null, null);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertNull(response.getId());
    assertNull(response.getName());
  }

  @Test
  @DisplayName("Constructor ignores parent field from Genre entity")
  void constructor_ignoresParentFieldFromGenreEntity() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    Genre childGenre = new Genre("2", "Science Fiction", parentGenre);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertEquals("2", response.getId());
    assertEquals("Science Fiction", response.getName());
    // Verify that ChildGenreResponse doesn't have a parent field by checking
    // available methods
    // This is implicit - the class only has id and name fields
  }

  @Test
  @DisplayName("Constructor handles Genre entity with empty string values")
  void constructor_handlesEmptyStringValues() {
    // Arrange
    Genre childGenre = new Genre("", "", null);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertEquals("", response.getId());
    assertEquals("", response.getName());
  }

  @Test
  @DisplayName("Constructor handles Genre entity with whitespace values")
  void constructor_handlesWhitespaceValues() {
    // Arrange
    Genre childGenre = new Genre("  ", "  ", null);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertEquals("  ", response.getId());
    assertEquals("  ", response.getName());
  }

  @Test
  @DisplayName("Constructor handles Genre entity with special characters")
  void constructor_handlesSpecialCharacters() {
    // Arrange
    Genre childGenre = new Genre("id-123_special", "Science Fiction & Fantasy", null);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertEquals("id-123_special", response.getId());
    assertEquals("Science Fiction & Fantasy", response.getName());
  }

  @Test
  @DisplayName("Constructor handles Genre entity with long string values")
  void constructor_handlesLongStringValues() {
    // Arrange
    String longId = "a".repeat(100);
    String longName = "Very Long Genre Name That Exceeds Normal Length Expectations For Testing Purposes";
    Genre childGenre = new Genre(longId, longName, null);

    // Act
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Assert
    assertEquals(longId, response.getId());
    assertEquals(longName, response.getName());
  }

  @Test
  @DisplayName("Setters and getters work correctly for all fields")
  void settersAndGetters_workCorrectlyForAllFields() {
    // Arrange
    Genre childGenre = new Genre("1", "Original Name", null);
    ChildGenreResponse response = new ChildGenreResponse(childGenre);

    // Act & Assert - Test setters and getters
    response.setId("new-id");
    assertEquals("new-id", response.getId());

    response.setName("New Name");
    assertEquals("New Name", response.getName());
  }

  @Test
  @DisplayName("Multiple ChildGenreResponse instances can be created independently")
  void multipleInstances_canBeCreatedIndependently() {
    // Arrange
    Genre child1 = new Genre("1", "Science Fiction", null);
    Genre child2 = new Genre("2", "Fantasy", null);

    // Act
    ChildGenreResponse response1 = new ChildGenreResponse(child1);
    ChildGenreResponse response2 = new ChildGenreResponse(child2);

    // Assert
    assertEquals("1", response1.getId());
    assertEquals("Science Fiction", response1.getName());

    assertEquals("2", response2.getId());
    assertEquals("Fantasy", response2.getName());

    // Verify they are independent
    response1.setName("Modified Science Fiction");
    assertEquals("Modified Science Fiction", response1.getName());
    assertEquals("Fantasy", response2.getName()); // Should remain unchanged
  }
}
