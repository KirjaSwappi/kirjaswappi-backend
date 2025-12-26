/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.kirjaswappi.backend.config.TestContainersConfig;
import com.kirjaswappi.backend.jpa.daos.GenreDao;
import com.kirjaswappi.backend.jpa.repositories.GenreRepository;

/**
 * Integration test for the Genre API endpoint. Tests the complete GET /genres
 * endpoint with real database operations to verify nested response format, HTTP
 * status codes, response headers, and behavior with various database states.
 */
@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GenreApiIntegrationTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private GenreRepository genreRepository;

  @Autowired
  private CacheManager cacheManager;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Clean up existing data
    genreRepository.deleteAll();

    // Clear caches to prevent leakage between tests
    if (cacheManager.getCache("genres") != null) {
      cacheManager.getCache("genres").clear();
    }
    if (cacheManager.getCache("nested_genres") != null) {
      cacheManager.getCache("nested_genres").clear();
    }
  }

  @Test
  @DisplayName("Should return empty nested structure when no genres exist")
  void shouldReturnEmptyNestedStructureWhenNoGenresExist() throws Exception {
    // Test with completely empty database
    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.parentGenres").isMap())
        .andExpect(jsonPath("$.parentGenres").isEmpty());
  }

  @Test
  @DisplayName("Should return nested structure with only parent genres when no child genres exist")
  void shouldReturnNestedStructureWithOnlyParentGenres() throws Exception {
    // Create parent genres only (no children)
    GenreDao fiction = createGenre("1", "Fiction", null);
    GenreDao nonFiction = createGenre("2", "Non-Fiction", null);
    GenreDao biography = createGenre("3", "Biography", null);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.parentGenres").isMap())
        .andExpect(jsonPath("$.parentGenres.Fiction.id").value("1"))
        .andExpect(jsonPath("$.parentGenres.Fiction.name").value("Fiction"))
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres").isEmpty())
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].id").value("2"))
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].name").value("Non-Fiction"))
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].childGenres").isEmpty())
        .andExpect(jsonPath("$.parentGenres.Biography.id").value("3"))
        .andExpect(jsonPath("$.parentGenres.Biography.name").value("Biography"))
        .andExpect(jsonPath("$.parentGenres.Biography.childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres.Biography.childGenres").isEmpty());
  }

  @Test
  @DisplayName("Should handle orphaned child genres gracefully")
  void shouldHandleOrphanedChildGenresGracefully() throws Exception {
    // Create child genres without their parent existing in database

    // Create a parent reference that doesn't exist in database
    var nonExistentParent = GenreDao.builder()
        .id("999")
        .name("Fiction")
        .build();

    GenreDao orphanedChild1 = GenreDao.builder()
        .id("1")
        .name("Science Fiction")
        .parent(nonExistentParent)
        .build();
    genreRepository.save(orphanedChild1);

    GenreDao orphanedChild2 = GenreDao.builder()
        .id("2")
        .name("Fantasy")
        .parent(nonExistentParent)
        .build();
    genreRepository.save(orphanedChild2);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.parentGenres").isMap())
        // Orphaned children should be treated as parent genres
        .andExpect(jsonPath("$.parentGenres['Science Fiction'].id").value("1"))
        .andExpect(jsonPath("$.parentGenres['Science Fiction'].name").value("Science Fiction"))
        .andExpect(jsonPath("$.parentGenres['Science Fiction'].childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres['Science Fiction'].childGenres").isEmpty())
        .andExpect(jsonPath("$.parentGenres.Fantasy.id").value("2"))
        .andExpect(jsonPath("$.parentGenres.Fantasy.name").value("Fantasy"))
        .andExpect(jsonPath("$.parentGenres.Fantasy.childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres.Fantasy.childGenres").isEmpty());
  }

  @Test
  @DisplayName("Should return proper nested structure with parent-child relationships")
  void shouldReturnProperNestedStructureWithParentChildRelationships() throws Exception {
    // Create parent genres
    GenreDao fiction = createGenre("1", "Fiction", null);
    GenreDao nonFiction = createGenre("2", "Non-Fiction", null);

    // Create child genres
    GenreDao scienceFiction = createGenre("3", "Science Fiction", fiction);
    GenreDao fantasy = createGenre("4", "Fantasy", fiction);
    GenreDao mystery = createGenre("5", "Mystery", fiction);
    GenreDao biography = createGenre("6", "Biography", nonFiction);
    GenreDao history = createGenre("7", "History", nonFiction);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.parentGenres").isMap())

        // Verify Fiction parent with its children
        .andExpect(jsonPath("$.parentGenres.Fiction.id").value("1"))
        .andExpect(jsonPath("$.parentGenres.Fiction.name").value("Fiction"))
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres.length()").value(3))
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres[?(@.name == 'Science Fiction')].id").value("3"))
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres[?(@.name == 'Fantasy')].id").value("4"))
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres[?(@.name == 'Mystery')].id").value("5"))

        // Verify Non-Fiction parent with its children
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].id").value("2"))
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].name").value("Non-Fiction"))
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].childGenres.length()").value(2))
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].childGenres[?(@.name == 'Biography')].id").value("6"))
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].childGenres[?(@.name == 'History')].id").value("7"));
  }

  @Test
  @DisplayName("Should return correct HTTP status and headers")
  void shouldReturnCorrectHttpStatusAndHeaders() throws Exception {
    // Create some test data
    GenreDao fiction = createGenre("1", "Fiction", null);
    GenreDao scienceFiction = createGenre("2", "Science Fiction", fiction);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Content-Type", "application/json"));
  }

  @Test
  @DisplayName("Should maintain consistent response structure across different data scenarios")
  void shouldMaintainConsistentResponseStructureAcrossDifferentDataScenarios() throws Exception {
    // Test 1: Empty database
    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentGenres").exists())
        .andExpect(jsonPath("$.parentGenres").isMap());

    // Test 2: Add single parent genre
    GenreDao fiction = createGenre("1", "Fiction", null);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentGenres").exists())
        .andExpect(jsonPath("$.parentGenres").isMap())
        .andExpect(jsonPath("$.parentGenres.Fiction").exists())
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres").isArray());

    // Test 3: Add child genre
    GenreDao scienceFiction = createGenre("2", "Science Fiction", fiction);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentGenres").exists())
        .andExpect(jsonPath("$.parentGenres").isMap())
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres.length()").value(1));
  }

  @Test
  @DisplayName("Should handle complex nested scenarios with multiple levels")
  void shouldHandleComplexNestedScenariosWithMultipleLevels() throws Exception {
    // Create a complex hierarchy
    GenreDao fiction = createGenre("1", "Fiction", null);
    GenreDao nonFiction = createGenre("2", "Non-Fiction", null);
    GenreDao reference = createGenre("3", "Reference", null);

    // Fiction children
    GenreDao scienceFiction = createGenre("4", "Science Fiction", fiction);
    GenreDao fantasy = createGenre("5", "Fantasy", fiction);
    GenreDao mystery = createGenre("6", "Mystery", fiction);
    GenreDao romance = createGenre("7", "Romance", fiction);

    // Non-Fiction children
    GenreDao biography = createGenre("8", "Biography", nonFiction);
    GenreDao history = createGenre("9", "History", nonFiction);
    GenreDao science = createGenre("10", "Science", nonFiction);

    // Reference has no children (empty childGenres array)

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.parentGenres").isMap())
        .andExpect(jsonPath("$.parentGenres.length()").value(3))

        // Verify Fiction and its 4 children
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres.length()").value(4))

        // Verify Non-Fiction and its 3 children
        .andExpect(jsonPath("$.parentGenres['Non-Fiction'].childGenres.length()").value(3))

        // Verify Reference with no children
        .andExpect(jsonPath("$.parentGenres.Reference.childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres.Reference.childGenres").isEmpty());
  }

  @Test
  @DisplayName("Should verify all genre fields are present in response")
  void shouldVerifyAllGenreFieldsArePresentInResponse() throws Exception {
    GenreDao fiction = createGenre("1", "Fiction", null);
    GenreDao scienceFiction = createGenre("2", "Science Fiction", fiction);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        // Verify parent genre has all required fields
        .andExpect(jsonPath("$.parentGenres.Fiction.id").exists())
        .andExpect(jsonPath("$.parentGenres.Fiction.name").exists())
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres").exists())

        // Verify child genre has all required fields (no parent reference to avoid
        // circular refs)
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres[0].id").exists())
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres[0].name").exists())

        // Verify child genre does NOT have parent field (to avoid circular references)
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres[0].parent").doesNotExist())
        .andExpect(jsonPath("$.parentGenres.Fiction.childGenres[0].childGenres").doesNotExist());
  }

  @Test
  @DisplayName("Should handle special characters in genre names")
  void shouldHandleSpecialCharactersInGenreNames() throws Exception {
    // Create genres with special characters
    GenreDao genreWithSpaces = createGenre("1", "Science Fiction", null);
    GenreDao genreWithHyphen = createGenre("2", "Non-Fiction", null);
    GenreDao genreWithApostrophe = createGenre("3", "Children's Books", null);
    GenreDao genreWithAmpersand = createGenre("4", "Arts & Crafts", null);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.parentGenres['Science Fiction']").exists())
        .andExpect(jsonPath("$.parentGenres['Non-Fiction']").exists())
        .andExpect(jsonPath("$.parentGenres[\"Children's Books\"]").exists())
        .andExpect(jsonPath("$.parentGenres['Arts & Crafts']").exists());
  }

  // Helper method to create and save a genre
  private GenreDao createGenre(String id, String name, GenreDao parent) {
    GenreDao genre = GenreDao.builder()
        .id(id)
        .name(name)
        .parent(parent)
        .build();
    GenreDao saved = genreRepository.save(genre);

    // Clear caches because direct repository save bypasses service eviction
    if (cacheManager.getCache("genres") != null) {
      cacheManager.getCache("genres").clear();
    }
    if (cacheManager.getCache("nested_genres") != null) {
      cacheManager.getCache("nested_genres").clear();
    }

    return saved;
  }

  // ========== POST /api/v1/genres - Create Genre Tests ==========

  @Nested
  @DisplayName("POST /api/v1/genres - Create Genre")
  class CreateGenreTests {

    @Test
    @DisplayName("Should create a parent genre successfully")
    void shouldCreateParentGenreSuccessfully() throws Exception {
      String requestBody = """
          {
            "name": "Fiction",
            "parentId": null
          }
          """;

      mockMvc.perform(post("/api/v1/genres")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.name").value("Fiction"));
    }

    @Test
    @DisplayName("Should create a child genre with parent reference")
    void shouldCreateChildGenreWithParentReference() throws Exception {
      // First create a parent genre
      GenreDao parent = createGenre("parent-id", "Fiction", null);

      String requestBody = """
          {
            "name": "Science Fiction",
            "parentId": "parent-id"
          }
          """;

      mockMvc.perform(post("/api/v1/genres")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.name").value("Science Fiction"));
    }

    @Test
    @DisplayName("Should create genre with empty parentId string as parent genre")
    void shouldCreateGenreWithEmptyParentIdAsParentGenre() throws Exception {
      String requestBody = """
          {
            "name": "Non-Fiction",
            "parentId": ""
          }
          """;

      mockMvc.perform(post("/api/v1/genres")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.name").value("Non-Fiction"));
    }

    @Test
    @DisplayName("Should return 400 when name is blank")
    void shouldReturn400WhenNameIsBlank() throws Exception {
      String requestBody = """
          {
            "name": "",
            "parentId": null
          }
          """;

      mockMvc.perform(post("/api/v1/genres")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when name is null")
    void shouldReturn400WhenNameIsNull() throws Exception {
      String requestBody = """
          {
            "name": null,
            "parentId": null
          }
          """;

      mockMvc.perform(post("/api/v1/genres")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when name is whitespace only")
    void shouldReturn400WhenNameIsWhitespaceOnly() throws Exception {
      String requestBody = """
          {
            "name": "   ",
            "parentId": null
          }
          """;

      mockMvc.perform(post("/api/v1/genres")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should create genre with special characters in name")
    void shouldCreateGenreWithSpecialCharactersInName() throws Exception {
      String requestBody = """
          {
            "name": "Children's Books & Literature",
            "parentId": null
          }
          """;

      mockMvc.perform(post("/api/v1/genres")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("Children's Books & Literature"));
    }

    @Test
    @DisplayName("Should return 400 for invalid JSON request body")
    void shouldReturn400ForInvalidJsonRequestBody() throws Exception {
      String invalidJson = "{ invalid json }";

      mockMvc.perform(post("/api/v1/genres")
          .contentType(MediaType.APPLICATION_JSON)
          .content(invalidJson))
          .andExpect(status().isBadRequest());
    }
  }

  // ========== PUT /api/v1/genres/{id} - Update Genre Tests ==========

  @Nested
  @DisplayName("PUT /api/v1/genres/{id} - Update Genre")
  class UpdateGenreTests {

    @Test
    @DisplayName("Should update genre name successfully")
    void shouldUpdateGenreNameSuccessfully() throws Exception {
      // Create a genre first
      GenreDao genre = createGenre("update-test-id", "Fiction", null);

      String requestBody = """
          {
            "id": "update-test-id",
            "name": "Updated Fiction",
            "parentId": null
          }
          """;

      mockMvc.perform(put("/api/v1/genres/update-test-id")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value("update-test-id"))
          .andExpect(jsonPath("$.name").value("Updated Fiction"));
    }

    @Test
    @DisplayName("Should update genre to have a parent")
    void shouldUpdateGenreToHaveParent() throws Exception {
      // Create parent and child genres
      GenreDao parent = createGenre("parent-genre-id", "Fiction", null);
      GenreDao child = createGenre("child-genre-id", "Science Fiction", null);

      String requestBody = """
          {
            "id": "child-genre-id",
            "name": "Science Fiction",
            "parentId": "parent-genre-id"
          }
          """;

      mockMvc.perform(put("/api/v1/genres/child-genre-id")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value("child-genre-id"))
          .andExpect(jsonPath("$.name").value("Science Fiction"));
    }

    @Test
    @DisplayName("Should return 400 when path id and body id mismatch")
    void shouldReturn400WhenPathIdAndBodyIdMismatch() throws Exception {
      GenreDao genre = createGenre("original-id", "Fiction", null);

      String requestBody = """
          {
            "id": "different-id",
            "name": "Updated Fiction",
            "parentId": null
          }
          """;

      mockMvc.perform(put("/api/v1/genres/original-id")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when name is blank on update")
    void shouldReturn400WhenNameIsBlankOnUpdate() throws Exception {
      GenreDao genre = createGenre("blank-name-test", "Fiction", null);

      String requestBody = """
          {
            "id": "blank-name-test",
            "name": "",
            "parentId": null
          }
          """;

      mockMvc.perform(put("/api/v1/genres/blank-name-test")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when id is blank on update")
    void shouldReturn400WhenIdIsBlankOnUpdate() throws Exception {
      String requestBody = """
          {
            "id": "",
            "name": "Fiction",
            "parentId": null
          }
          """;

      mockMvc.perform(put("/api/v1/genres/some-id")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when updating non-existent genre")
    void shouldReturn400WhenUpdatingNonExistentGenre() throws Exception {
      String requestBody = """
          {
            "id": "non-existent-id",
            "name": "Updated Genre",
            "parentId": null
          }
          """;

      mockMvc.perform(put("/api/v1/genres/non-existent-id")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should update genre to remove parent")
    void shouldUpdateGenreToRemoveParent() throws Exception {
      // Create parent and child
      GenreDao parent = createGenre("parent-remove-id", "Fiction", null);
      GenreDao child = createGenre("child-remove-parent", "Science Fiction", parent);

      String requestBody = """
          {
            "id": "child-remove-parent",
            "name": "Science Fiction",
            "parentId": null
          }
          """;

      mockMvc.perform(put("/api/v1/genres/child-remove-parent")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value("child-remove-parent"))
          .andExpect(jsonPath("$.name").value("Science Fiction"));
    }
  }

  // ========== DELETE /api/v1/genres/{id} - Delete Genre Tests ==========

  @Nested
  @DisplayName("DELETE /api/v1/genres/{id} - Delete Genre")
  class DeleteGenreTests {

    @Test
    @DisplayName("Should delete genre successfully")
    void shouldDeleteGenreSuccessfully() throws Exception {
      GenreDao genre = createGenre("delete-test-id", "Fiction", null);

      mockMvc.perform(delete("/api/v1/genres/delete-test-id"))
          .andExpect(status().isNoContent());

      // Verify genre is deleted by trying to get it via nested response
      mockMvc.perform(get("/api/v1/genres"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.parentGenres.Fiction").doesNotExist());
    }

    @Test
    @DisplayName("Should delete parent genre with child genres")
    void shouldDeleteParentGenreWithChildGenres() throws Exception {
      GenreDao parent = createGenre("parent-to-delete", "Fiction", null);
      GenreDao child1 = createGenre("child-1", "Science Fiction", parent);
      GenreDao child2 = createGenre("child-2", "Fantasy", parent);

      mockMvc.perform(delete("/api/v1/genres/parent-to-delete"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should delete child genre leaving parent intact")
    void shouldDeleteChildGenreLeavingParentIntact() throws Exception {
      GenreDao parent = createGenre("parent-intact", "Fiction", null);
      GenreDao child = createGenre("child-to-delete", "Science Fiction", parent);

      mockMvc.perform(delete("/api/v1/genres/child-to-delete"))
          .andExpect(status().isNoContent());

      // Verify parent still exists
      mockMvc.perform(get("/api/v1/genres"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.parentGenres.Fiction").exists())
          .andExpect(jsonPath("$.parentGenres.Fiction.id").value("parent-intact"));
    }

    @Test
    @DisplayName("Should return 400 when deleting non-existent genre")
    void shouldReturn400WhenDeletingNonExistentGenre() throws Exception {
      mockMvc.perform(delete("/api/v1/genres/non-existent-genre-id"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle deleting genre with special characters in id")
    void shouldHandleDeletingGenreWithSpecialId() throws Exception {
      GenreDao genre = createGenre("special-id-123", "Test Genre", null);

      mockMvc.perform(delete("/api/v1/genres/special-id-123"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should delete multiple genres in sequence")
    void shouldDeleteMultipleGenresInSequence() throws Exception {
      GenreDao genre1 = createGenre("seq-delete-1", "Genre 1", null);
      GenreDao genre2 = createGenre("seq-delete-2", "Genre 2", null);
      GenreDao genre3 = createGenre("seq-delete-3", "Genre 3", null);

      mockMvc.perform(delete("/api/v1/genres/seq-delete-1"))
          .andExpect(status().isNoContent());

      mockMvc.perform(delete("/api/v1/genres/seq-delete-2"))
          .andExpect(status().isNoContent());

      mockMvc.perform(delete("/api/v1/genres/seq-delete-3"))
          .andExpect(status().isNoContent());

      // Verify all are deleted
      mockMvc.perform(get("/api/v1/genres"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.parentGenres").isEmpty());
    }
  }
}
