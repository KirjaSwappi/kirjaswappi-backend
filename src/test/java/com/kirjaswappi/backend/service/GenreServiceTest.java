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
import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.GenreDao;
import com.kirjaswappi.backend.jpa.daos.SwapConditionDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
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
    var dao1 = new GenreDao()
        .id("1")
        .name("Fantasy");
    var dao2 = new GenreDao()
        .id("2")
        .name("SciFi");
    when(genreRepository.findAll()).thenReturn(List.of(dao1, dao2));
    List<Genre> genres = genreService.getGenres();
    assertEquals(2, genres.size());
  }

  @Test
  @DisplayName("Updates an existing genre")
  void updateGenreUpdatesGenre() {
    var dao = new GenreDao()
        .id("1")
        .name("Fantasy");
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
    var dao = new GenreDao().id("1");
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

  @Test
  @DisplayName("getGenreById returns genre when found")
  void getGenreByIdReturnsGenre() {
    // Arrange
    GenreDao dao = createGenreDao("1", "Fantasy", null);
    when(genreRepository.findById("1")).thenReturn(Optional.of(dao));

    // Act
    Genre result = genreService.getGenreById("1");

    // Assert
    assertNotNull(result);
    assertEquals("1", result.getId());
    assertEquals("Fantasy", result.getName());
    assertNull(result.getParent());
    verify(genreRepository, times(1)).findById("1");
  }

  @Test
  @DisplayName("getGenreById throws GenreNotFoundException when not found")
  void getGenreByIdThrowsWhenNotFound() {
    // Arrange
    when(genreRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(GenreNotFoundException.class, () -> {
      genreService.getGenreById("nonexistent");
    });
    verify(genreRepository, times(1)).findById("nonexistent");
  }

  @Test
  @DisplayName("getGenreByName returns genre when found")
  void getGenreByNameReturnsGenre() {
    // Arrange
    GenreDao dao = createGenreDao("1", "Science Fiction", null);
    when(genreRepository.findByName("Science Fiction")).thenReturn(Optional.of(dao));

    // Act
    Genre result = genreService.getGenreByName("Science Fiction");

    // Assert
    assertNotNull(result);
    assertEquals("1", result.getId());
    assertEquals("Science Fiction", result.getName());
    assertNull(result.getParent());
    verify(genreRepository, times(1)).findByName("Science Fiction");
  }

  @Test
  @DisplayName("getGenreByName throws GenreNotFoundException when not found")
  void getGenreByNameThrowsWhenNotFound() {
    // Arrange
    when(genreRepository.findByName("Nonexistent Genre")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(GenreNotFoundException.class, () -> {
      genreService.getGenreByName("Nonexistent Genre");
    });
    verify(genreRepository, times(1)).findByName("Nonexistent Genre");
  }

  @Test
  @DisplayName("addGenre with parent successfully adds child genre")
  void addGenreWithParentSuccess() {
    // Arrange
    GenreDao parentDao = createGenreDao("1", "Fiction", null);
    Genre parentGenre = new Genre("1", "Fiction", null);
    Genre childGenre = new Genre(null, "Science Fiction", parentGenre);

    when(genreRepository.existsByName("Science Fiction")).thenReturn(false);
    when(genreRepository.findById("1")).thenReturn(Optional.of(parentDao));
    when(genreRepository.save(any())).thenAnswer(invocation -> {
      GenreDao saved = invocation.getArgument(0);
      saved.setId("2");
      return saved;
    });

    // Act
    Genre result = genreService.addGenre(childGenre);

    // Assert
    assertNotNull(result);
    assertEquals("Science Fiction", result.getName());
    verify(genreRepository, times(1)).existsByName("Science Fiction");
    verify(genreRepository, times(1)).findById("1");
    verify(genreRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("addGenre with non-existent parent throws GenreNotFoundException")
  void addGenreWithNonExistentParentThrows() {
    // Arrange
    Genre parentGenre = new Genre("nonexistent", "Fiction", null);
    Genre childGenre = new Genre(null, "Science Fiction", parentGenre);

    when(genreRepository.existsByName("Science Fiction")).thenReturn(false);
    when(genreRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(GenreNotFoundException.class, () -> {
      genreService.addGenre(childGenre);
    });
    verify(genreRepository, times(1)).existsByName("Science Fiction");
    verify(genreRepository, times(1)).findById("nonexistent");
    verify(genreRepository, never()).save(any());
  }

  @Test
  @DisplayName("updateGenre with parent successfully updates genre")
  void updateGenreWithParentSuccess() {
    // Arrange
    GenreDao existingDao = createGenreDao("2", "SciFi", null);
    GenreDao parentDao = createGenreDao("1", "Fiction", null);

    Genre parentGenre = new Genre("1", "Fiction", null);
    Genre updatedGenre = new Genre("2", "Science Fiction", parentGenre);

    when(genreRepository.findById("2")).thenReturn(Optional.of(existingDao));
    when(genreRepository.findById("1")).thenReturn(Optional.of(parentDao));
    when(genreRepository.save(any())).thenReturn(existingDao);

    // Act
    Genre result = genreService.updateGenre(updatedGenre);

    // Assert
    assertNotNull(result);
    verify(genreRepository, times(1)).findById("2");
    verify(genreRepository, times(1)).findById("1");
    verify(genreRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("updateGenre removes parent when set to null")
  void updateGenreRemovesParent() {
    // Arrange
    GenreDao parentDao = createGenreDao("1", "Fiction", null);
    GenreDao existingDao = createGenreDao("2", "Science Fiction", "1");
    existingDao.setParent(parentDao);

    Genre updatedGenre = new Genre("2", "Science Fiction", null);

    when(genreRepository.findById("2")).thenReturn(Optional.of(existingDao));
    when(genreRepository.save(any())).thenReturn(existingDao);

    // Act
    Genre result = genreService.updateGenre(updatedGenre);

    // Assert
    assertNotNull(result);
    verify(genreRepository, times(1)).findById("2");
    verify(genreRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("updateGenre with non-existent parent throws GenreNotFoundException")
  void updateGenreWithNonExistentParentThrows() {
    // Arrange
    GenreDao existingDao = createGenreDao("2", "Science Fiction", null);
    Genre parentGenre = new Genre("nonexistent", "Fiction", null);
    Genre updatedGenre = new Genre("2", "Science Fiction", parentGenre);

    when(genreRepository.findById("2")).thenReturn(Optional.of(existingDao));
    when(genreRepository.findById("nonexistent")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(GenreNotFoundException.class, () -> {
      genreService.updateGenre(updatedGenre);
    });
    verify(genreRepository, times(1)).findById("2");
    verify(genreRepository, times(1)).findById("nonexistent");
    verify(genreRepository, never()).save(any());
  }

  @Test
  @DisplayName("deleteGenre throws GenreCannotBeDeletedException when genre is in user's favorite genres")
  void deleteGenreThrowsWhenInFavoriteGenres() {
    // Arrange
    GenreDao genreDao = createGenreDao("1", "Fantasy", null);
    UserDao userDao = new UserDao();
    userDao.setId("user1");
    userDao.setFavGenres(List.of(genreDao));
    userDao.setBooks(new ArrayList<>());

    when(genreRepository.existsById("1")).thenReturn(true);
    when(userRepository.findAll()).thenReturn(List.of(userDao));

    // Act & Assert
    assertThrows(com.kirjaswappi.backend.service.exceptions.GenreCannotBeDeletedException.class, () -> {
      genreService.deleteGenre("1");
    });
    verify(genreRepository, times(1)).existsById("1");
    verify(userRepository, times(1)).findAll();
    verify(genreRepository, never()).deleteById("1");
  }

  @Test
  @DisplayName("deleteGenre throws GenreCannotBeDeletedException when genre is in book genres")
  void deleteGenreThrowsWhenInBookGenres() {
    // Arrange
    GenreDao genreDao = createGenreDao("1", "Fantasy", null);

    BookDao bookDao = new BookDao();
    bookDao.setId("book1");
    bookDao.setGenres(List.of(genreDao));

    SwapConditionDao swapCondition = new SwapConditionDao();
    swapCondition.setSwappableGenres(List.of(genreDao));
    bookDao.setSwapCondition(swapCondition);

    UserDao userDao = new UserDao();
    userDao.setId("user1");
    userDao.setFavGenres(new ArrayList<>());
    userDao.setBooks(List.of(bookDao));

    when(genreRepository.existsById("1")).thenReturn(true);
    when(userRepository.findAll()).thenReturn(List.of(userDao));

    // Act & Assert
    assertThrows(com.kirjaswappi.backend.service.exceptions.GenreCannotBeDeletedException.class, () -> {
      genreService.deleteGenre("1");
    });
    verify(genreRepository, times(1)).existsById("1");
    verify(userRepository, times(1)).findAll();
    verify(genreRepository, never()).deleteById("1");
  }

  @Test
  @DisplayName("deleteGenre throws GenreCannotBeDeletedException when genre is only in swappable genres")
  void deleteGenreThrowsWhenOnlyInSwappableGenres() {
    // Arrange
    GenreDao genreDao = createGenreDao("1", "Fantasy", null);
    GenreDao otherGenreDao = createGenreDao("2", "SciFi", null);

    BookDao bookDao = new BookDao();
    bookDao.setId("book1");
    bookDao.setGenres(List.of(otherGenreDao)); // Genre is NOT in book's genres

    SwapConditionDao swapCondition = new SwapConditionDao();
    swapCondition.setSwappableGenres(List.of(genreDao)); // Genre is in swappable genres
    bookDao.setSwapCondition(swapCondition);

    UserDao userDao = new UserDao();
    userDao.setId("user1");
    userDao.setFavGenres(new ArrayList<>());
    userDao.setBooks(List.of(bookDao));

    when(genreRepository.existsById("1")).thenReturn(true);
    when(userRepository.findAll()).thenReturn(List.of(userDao));

    // Act & Assert
    assertThrows(com.kirjaswappi.backend.service.exceptions.GenreCannotBeDeletedException.class, () -> {
      genreService.deleteGenre("1");
    });
    verify(genreRepository, times(1)).existsById("1");
    verify(userRepository, times(1)).findAll();
    verify(genreRepository, never()).deleteById("1");
  }

  @Test
  @DisplayName("deleteGenre succeeds when genre is not being used")
  void deleteGenreSucceedsWhenNotUsed() {
    // Arrange
    UserDao userDao = new UserDao();
    userDao.setId("user1");
    userDao.setFavGenres(new ArrayList<>());
    userDao.setBooks(new ArrayList<>());

    when(genreRepository.existsById("1")).thenReturn(true);
    when(userRepository.findAll()).thenReturn(List.of(userDao));
    doNothing().when(genreRepository).deleteById("1");

    // Act
    genreService.deleteGenre("1");

    // Assert
    verify(genreRepository, times(1)).existsById("1");
    verify(userRepository, times(1)).findAll();
    verify(genreRepository, times(1)).deleteById("1");
  }

  @Test
  @DisplayName("deleteGenre handles null favGenres gracefully")
  void deleteGenreHandlesNullFavGenres() {
    // Arrange
    UserDao userDao = new UserDao();
    userDao.setId("user1");
    userDao.setFavGenres(null);
    userDao.setBooks(new ArrayList<>());

    when(genreRepository.existsById("1")).thenReturn(true);
    when(userRepository.findAll()).thenReturn(List.of(userDao));
    doNothing().when(genreRepository).deleteById("1");

    // Act
    genreService.deleteGenre("1");

    // Assert
    verify(genreRepository, times(1)).existsById("1");
    verify(userRepository, times(1)).findAll();
    verify(genreRepository, times(1)).deleteById("1");
  }

  @Test
  @DisplayName("deleteGenre handles null books gracefully")
  void deleteGenreHandlesNullBooks() {
    // Arrange
    UserDao userDao = new UserDao();
    userDao.setId("user1");
    userDao.setFavGenres(new ArrayList<>());
    userDao.setBooks(null);

    when(genreRepository.existsById("1")).thenReturn(true);
    when(userRepository.findAll()).thenReturn(List.of(userDao));
    doNothing().when(genreRepository).deleteById("1");

    // Act
    genreService.deleteGenre("1");

    // Assert
    verify(genreRepository, times(1)).existsById("1");
    verify(userRepository, times(1)).findAll();
    verify(genreRepository, times(1)).deleteById("1");
  }

  /**
   * Helper method to create GenreDao objects for testing
   */
  private GenreDao createGenreDao(String id, String name, String parentId) {
    GenreDao dao = new GenreDao()
        .id(id)
        .name(name);

    if (parentId != null) {
      GenreDao parent = new GenreDao();
      parent.id(parentId);
      dao.parent(parent);
    }

    return dao;
  }
}
