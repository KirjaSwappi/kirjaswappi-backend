/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.kirjaswappi.backend.http.dtos.responses.NestedGenresResponse;
import com.kirjaswappi.backend.http.dtos.responses.ParentGenreResponse;
import com.kirjaswappi.backend.jpa.daos.GenreDao;
import com.kirjaswappi.backend.jpa.repositories.GenreRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.exceptions.GenreAlreadyExistsException;
import com.kirjaswappi.backend.service.exceptions.GenreNotFoundException;

class GenreServiceTest {
  @Mock
  private GenreRepository genreRepository;
  @Mock
  private UserRepository userRepository;
  @InjectMocks
  private GenreService genreService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("Throws when genre already exists")
  void addGenreThrowsWhenExists() {
    when(genreRepository.existsByName("Fantasy")).thenReturn(true);
    Genre genre = new Genre("1", "Fantasy", null);
    assertThrows(GenreAlreadyExistsException.class, () -> {
      genreService.addGenre(genre);
    });
  }

  @Test
  @DisplayName("Adds genre successfully")
  void addGenreSuccess() {
    when(genreRepository.existsByName("SciFi")).thenReturn(false);
    Genre genre = new Genre("2", "SciFi", null);
    when(genreRepository.save(any())).thenReturn(new GenreDao());
    assertNotNull(genreService.addGenre(genre));
  }

  @Test
  @DisplayName("Returns list of genres")
  void getGenresReturnsList() {
    var dao1 = new GenreDao();
    dao1.setId("1");
    dao1.setName("Fantasy");
    var dao2 = new GenreDao();
    dao2.setId("2");
    dao2.setName("SciFi");
    when(genreRepository.findAll()).thenReturn(List.of(dao1, dao2));
    List<Genre> genres = genreService.getGenres();
    assertEquals(2, genres.size());
  }

  @Test
  @DisplayName("Updates an existing genre")
  void updateGenreUpdatesGenre() {
    var dao = new GenreDao();
    dao.setId("1");
    dao.setName("Fantasy");
    when(genreRepository.findById("1")).thenReturn(Optional.of(dao));
    when(genreRepository.save(any())).thenReturn(dao);
    Genre genre = new Genre("1", "FantasyUpdated", null);
    assertNotNull(genreService.updateGenre(genre));
  }

  @Test
  @DisplayName("Throws when updating a non-existent genre")
  void updateGenreThrowsWhenNotFound() {
    when(genreRepository.findById("1")).thenReturn(Optional.empty());
    Genre genre = new Genre("1", "FantasyUpdated", null);
    assertThrows(GenreNotFoundException.class, () -> {
      genreService.updateGenre(genre);
    });
  }

  @Test
  @DisplayName("Deletes a genre by ID")
  void deleteGenreDeletesGenre() {
    var dao = new GenreDao();
    dao.setId("1");
    when(genreRepository.findById("1")).thenReturn(Optional.of(dao));
    when(genreRepository.existsById("1")).thenReturn(true);
    doNothing().when(genreRepository).deleteById("1");
    when(userRepository.findAll()).thenReturn(List.of());
    genreService.deleteGenre("1");
    verify(genreRepository, times(1)).deleteById("1");
  }

  @Test
  @DisplayName("Throws when deleting a non-existent genre")
  void deleteGenreThrowsWhenNotFound() {
    when(genreRepository.findById("1")).thenReturn(Optional.empty());
    assertThrows(GenreNotFoundException.class, () -> {
      genreService.deleteGenre("1");
    });
  }

  // Comprehensive unit tests for getNestedGenres() method

  @Test
  @DisplayName("getNestedGenres returns empty response when no genres exist")
  void getNestedGenresWithEmptyGenreList() {
    // Arrange
    when(genreRepository.findAll()).thenReturn(new ArrayList<>());

    // Act
    NestedGenresResponse response = genreService.getNestedGenres();

    // Assert
    assertNotNull(response);
    assertNotNull(response.getParentGenres());
    assertTrue(response.getParentGenres().isEmpty());
  }

  @Test
  @DisplayName("getNestedGenres returns only parent genres when no children exist")
  void getNestedGenresWithOnlyParentGenres() {
    // Arrange
    GenreDao fictionDao = createGenreDao("1", "Fiction", null);
    GenreDao nonFictionDao = createGenreDao("2", "Non-Fiction", null);
    GenreDao biographyDao = createGenreDao("3", "Biography", null);

    when(genreRepository.findAll()).thenReturn(List.of(fictionDao, nonFictionDao, biographyDao));

    // Act
    NestedGenresResponse response = genreService.getNestedGenres();

    // Assert
    assertNotNull(response);
    Map<String, ParentGenreResponse> parentGenres = response.getParentGenres();
    assertEquals(3, parentGenres.size());

    // Verify Fiction parent genre
    assertTrue(parentGenres.containsKey("Fiction"));
    ParentGenreResponse fiction = parentGenres.get("Fiction");
    assertEquals("1", fiction.getId());
    assertEquals("Fiction", fiction.getName());
    assertTrue(fiction.getChildGenres().isEmpty());

    // Verify Non-Fiction parent genre
    assertTrue(parentGenres.containsKey("Non-Fiction"));
    ParentGenreResponse nonFiction = parentGenres.get("Non-Fiction");
    assertEquals("2", nonFiction.getId());
    assertEquals("Non-Fiction", nonFiction.getName());
    assertTrue(nonFiction.getChildGenres().isEmpty());

    // Verify Biography parent genre
    assertTrue(parentGenres.containsKey("Biography"));
    ParentGenreResponse biography = parentGenres.get("Biography");
    assertEquals("3", biography.getId());
    assertEquals("Biography", biography.getName());
    assertTrue(biography.getChildGenres().isEmpty());
  }

  @Test
  @DisplayName("getNestedGenres handles orphaned children gracefully")
  void getNestedGenresWithOrphanedChildren() {
    // Arrange - Create child genres with non-existent parent references
    GenreDao parentDao = createGenreDao("parent1", "Fiction", null);
    GenreDao orphanedChildDao = createGenreDao("child1", "Science Fiction", "nonexistent-parent");

    when(genreRepository.findAll()).thenReturn(List.of(parentDao, orphanedChildDao));

    // Act
    NestedGenresResponse response = genreService.getNestedGenres();

    // Assert
    assertNotNull(response);
    Map<String, ParentGenreResponse> parentGenres = response.getParentGenres();
    assertEquals(2, parentGenres.size());

    // Verify the actual parent genre
    assertTrue(parentGenres.containsKey("Fiction"));
    ParentGenreResponse fiction = parentGenres.get("Fiction");
    assertEquals("parent1", fiction.getId());
    assertEquals("Fiction", fiction.getName());
    assertTrue(fiction.getChildGenres().isEmpty());

    // Verify orphaned child is treated as parent genre
    assertTrue(parentGenres.containsKey("Science Fiction"));
    ParentGenreResponse orphanedAsParent = parentGenres.get("Science Fiction");
    assertEquals("child1", orphanedAsParent.getId());
    assertEquals("Science Fiction", orphanedAsParent.getName());
    assertTrue(orphanedAsParent.getChildGenres().isEmpty());
  }

  @Test
  @DisplayName("getNestedGenres builds proper parent-child relationships")
  void getNestedGenresWithProperParentChildRelationships() {
    // Arrange
    GenreDao fictionDao = createGenreDao("1", "Fiction", null);
    GenreDao nonFictionDao = createGenreDao("2", "Non-Fiction", null);
    GenreDao sciFiDao = createGenreDao("3", "Science Fiction", "1");
    GenreDao fantasyDao = createGenreDao("4", "Fantasy", "1");
    GenreDao biographyDao = createGenreDao("5", "Biography", "2");

    when(genreRepository.findAll()).thenReturn(List.of(fictionDao, nonFictionDao, sciFiDao, fantasyDao, biographyDao));

    // Act
    NestedGenresResponse response = genreService.getNestedGenres();

    // Assert
    assertNotNull(response);
    Map<String, ParentGenreResponse> parentGenres = response.getParentGenres();
    assertEquals(2, parentGenres.size()); // Only Fiction and Non-Fiction should be top-level

    // Verify Fiction parent with its children
    assertTrue(parentGenres.containsKey("Fiction"));
    ParentGenreResponse fiction = parentGenres.get("Fiction");
    assertEquals("1", fiction.getId());
    assertEquals("Fiction", fiction.getName());
    assertEquals(2, fiction.getChildGenres().size());

    // Verify Fiction's children
    var fictionChildren = fiction.getChildGenres();
    assertTrue(fictionChildren.stream()
        .anyMatch(child -> child.getId().equals("3") && child.getName().equals("Science Fiction")));
    assertTrue(
        fictionChildren.stream().anyMatch(child -> child.getId().equals("4") && child.getName().equals("Fantasy")));

    // Verify Non-Fiction parent with its child
    assertTrue(parentGenres.containsKey("Non-Fiction"));
    ParentGenreResponse nonFiction = parentGenres.get("Non-Fiction");
    assertEquals("2", nonFiction.getId());
    assertEquals("Non-Fiction", nonFiction.getName());
    assertEquals(1, nonFiction.getChildGenres().size());

    // Verify Non-Fiction's child
    var nonFictionChildren = nonFiction.getChildGenres();
    assertTrue(nonFictionChildren.stream()
        .anyMatch(child -> child.getId().equals("5") && child.getName().equals("Biography")));
  }

  @Test
  @DisplayName("getNestedGenres handles mixed scenarios with parents, children, and orphans")
  void getNestedGenresWithMixedScenarios() {
    // Arrange - Mix of valid parent-child relationships, orphaned children, and
    // standalone parents
    GenreDao fictionDao = createGenreDao("1", "Fiction", null);
    GenreDao standaloneDao = createGenreDao("2", "Standalone", null);
    GenreDao sciFiDao = createGenreDao("3", "Science Fiction", "1");
    GenreDao orphanedDao = createGenreDao("4", "Orphaned Genre", "nonexistent");

    when(genreRepository.findAll()).thenReturn(List.of(fictionDao, standaloneDao, sciFiDao, orphanedDao));

    // Act
    NestedGenresResponse response = genreService.getNestedGenres();

    // Assert
    assertNotNull(response);
    Map<String, ParentGenreResponse> parentGenres = response.getParentGenres();
    assertEquals(3, parentGenres.size()); // Fiction, Standalone, and Orphaned Genre

    // Verify Fiction with its child
    assertTrue(parentGenres.containsKey("Fiction"));
    ParentGenreResponse fiction = parentGenres.get("Fiction");
    assertEquals("1", fiction.getId());
    assertEquals("Fiction", fiction.getName());
    assertEquals(1, fiction.getChildGenres().size());
    assertEquals("3", fiction.getChildGenres().get(0).getId());
    assertEquals("Science Fiction", fiction.getChildGenres().get(0).getName());

    // Verify standalone parent
    assertTrue(parentGenres.containsKey("Standalone"));
    ParentGenreResponse standalone = parentGenres.get("Standalone");
    assertEquals("2", standalone.getId());
    assertEquals("Standalone", standalone.getName());
    assertTrue(standalone.getChildGenres().isEmpty());

    // Verify orphaned child treated as parent
    assertTrue(parentGenres.containsKey("Orphaned Genre"));
    ParentGenreResponse orphaned = parentGenres.get("Orphaned Genre");
    assertEquals("4", orphaned.getId());
    assertEquals("Orphaned Genre", orphaned.getName());
    assertTrue(orphaned.getChildGenres().isEmpty());
  }

  @Test
  @DisplayName("getNestedGenres handles single parent with multiple children")
  void getNestedGenresWithSingleParentMultipleChildren() {
    // Arrange
    GenreDao fictionDao = createGenreDao("1", "Fiction", null);
    GenreDao sciFiDao = createGenreDao("2", "Science Fiction", "1");
    GenreDao fantasyDao = createGenreDao("3", "Fantasy", "1");
    GenreDao mysteryDao = createGenreDao("4", "Mystery", "1");
    GenreDao thrillerDao = createGenreDao("5", "Thriller", "1");

    when(genreRepository.findAll()).thenReturn(List.of(fictionDao, sciFiDao, fantasyDao, mysteryDao, thrillerDao));

    // Act
    NestedGenresResponse response = genreService.getNestedGenres();

    // Assert
    assertNotNull(response);
    Map<String, ParentGenreResponse> parentGenres = response.getParentGenres();
    assertEquals(1, parentGenres.size());

    ParentGenreResponse fiction = parentGenres.get("Fiction");
    assertNotNull(fiction);
    assertEquals("1", fiction.getId());
    assertEquals("Fiction", fiction.getName());
    assertEquals(4, fiction.getChildGenres().size());

    // Verify all children are present
    var childNames = fiction.getChildGenres().stream().map(child -> child.getName()).toList();
    assertTrue(childNames.contains("Science Fiction"));
    assertTrue(childNames.contains("Fantasy"));
    assertTrue(childNames.contains("Mystery"));
    assertTrue(childNames.contains("Thriller"));
  }

  /**
   * Helper method to create GenreDao objects for testing
   */
  private GenreDao createGenreDao(String id, String name, String parentId) {
    GenreDao dao = new GenreDao();
    dao.setId(id);
    dao.setName(name);

    if (parentId != null) {
      GenreDao parent = new GenreDao();
      parent.setId(parentId);
      dao.setParent(parent);
    }

    return dao;
  }
}
