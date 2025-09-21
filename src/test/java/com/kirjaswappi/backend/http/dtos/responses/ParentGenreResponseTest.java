/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kirjaswappi.backend.service.entities.Genre;

class ParentGenreResponseTest {

  @Test
  @DisplayName("Constructor creates ParentGenreResponse with parent genre and child genres")
  void constructor_createsResponseWithParentAndChildren() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    Genre childGenre1 = new Genre("2", "Science Fiction", parentGenre);
    Genre childGenre2 = new Genre("3", "Fantasy", parentGenre);
    List<Genre> children = List.of(childGenre1, childGenre2);

    // Act
    ParentGenreResponse response = new ParentGenreResponse(parentGenre, children);

    // Assert
    assertEquals("1", response.getId());
    assertEquals("Fiction", response.getName());
    assertNotNull(response.getChildGenres());
    assertEquals(2, response.getChildGenres().size());

    // Verify first child
    ChildGenreResponse child1 = response.getChildGenres().get(0);
    assertEquals("2", child1.getId());
    assertEquals("Science Fiction", child1.getName());

    // Verify second child
    ChildGenreResponse child2 = response.getChildGenres().get(1);
    assertEquals("3", child2.getId());
    assertEquals("Fantasy", child2.getName());
  }

  @Test
  @DisplayName("Constructor handles parent genre with empty children list")
  void constructor_handlesEmptyChildrenList() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    List<Genre> emptyChildren = new ArrayList<>();

    // Act
    ParentGenreResponse response = new ParentGenreResponse(parentGenre, emptyChildren);

    // Assert
    assertEquals("1", response.getId());
    assertEquals("Fiction", response.getName());
    assertNotNull(response.getChildGenres());
    assertTrue(response.getChildGenres().isEmpty());
    assertEquals(0, response.getChildGenres().size());
  }

  @Test
  @DisplayName("Constructor handles null parent genre fields gracefully")
  void constructor_handlesNullParentGenreFields() {
    // Arrange
    Genre parentGenre = new Genre(null, null, null);
    List<Genre> children = List.of();

    // Act
    ParentGenreResponse response = new ParentGenreResponse(parentGenre, children);

    // Assert
    assertNull(response.getId());
    assertNull(response.getName());
    assertNotNull(response.getChildGenres());
    assertTrue(response.getChildGenres().isEmpty());
  }

  @Test
  @DisplayName("Constructor properly maps Genre entity fields to response fields")
  void constructor_mapsGenreEntityFieldsCorrectly() {
    // Arrange
    Genre parentGenre = new Genre("parent-123", "Historical Fiction", null);
    Genre childGenre = new Genre("child-456", "World War II Fiction", parentGenre);
    List<Genre> children = List.of(childGenre);

    // Act
    ParentGenreResponse response = new ParentGenreResponse(parentGenre, children);

    // Assert
    assertEquals("parent-123", response.getId());
    assertEquals("Historical Fiction", response.getName());
    assertEquals(1, response.getChildGenres().size());

    ChildGenreResponse childResponse = response.getChildGenres().get(0);
    assertEquals("child-456", childResponse.getId());
    assertEquals("World War II Fiction", childResponse.getName());
  }

  @Test
  @DisplayName("Constructor handles single child genre")
  void constructor_handlesSingleChildGenre() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    Genre singleChild = new Genre("2", "Mystery", parentGenre);
    List<Genre> children = List.of(singleChild);

    // Act
    ParentGenreResponse response = new ParentGenreResponse(parentGenre, children);

    // Assert
    assertEquals("1", response.getId());
    assertEquals("Fiction", response.getName());
    assertEquals(1, response.getChildGenres().size());

    ChildGenreResponse child = response.getChildGenres().get(0);
    assertEquals("2", child.getId());
    assertEquals("Mystery", child.getName());
  }

  @Test
  @DisplayName("Constructor handles multiple child genres with various names")
  void constructor_handlesMultipleChildGenresWithVariousNames() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    Genre child1 = new Genre("2", "Science Fiction", parentGenre);
    Genre child2 = new Genre("3", "Fantasy", parentGenre);
    Genre child3 = new Genre("4", "Mystery", parentGenre);
    Genre child4 = new Genre("5", "Romance", parentGenre);
    List<Genre> children = List.of(child1, child2, child3, child4);

    // Act
    ParentGenreResponse response = new ParentGenreResponse(parentGenre, children);

    // Assert
    assertEquals("1", response.getId());
    assertEquals("Fiction", response.getName());
    assertEquals(4, response.getChildGenres().size());

    // Verify all children are properly mapped
    List<String> childNames = response.getChildGenres().stream()
        .map(ChildGenreResponse::getName)
        .toList();
    assertTrue(childNames.contains("Science Fiction"));
    assertTrue(childNames.contains("Fantasy"));
    assertTrue(childNames.contains("Mystery"));
    assertTrue(childNames.contains("Romance"));
  }

  @Test
  @DisplayName("Setters and getters work correctly for all fields")
  void settersAndGetters_workCorrectlyForAllFields() {
    // Arrange
    Genre parentGenre = new Genre("1", "Fiction", null);
    ParentGenreResponse response = new ParentGenreResponse(parentGenre, List.of());

    // Act & Assert - Test setters and getters
    response.setId("new-id");
    assertEquals("new-id", response.getId());

    response.setName("New Name");
    assertEquals("New Name", response.getName());

    List<ChildGenreResponse> newChildren = List.of(
        new ChildGenreResponse(new Genre("child1", "Child 1", null)));
    response.setChildGenres(newChildren);
    assertEquals(newChildren, response.getChildGenres());
    assertEquals(1, response.getChildGenres().size());
  }
}
