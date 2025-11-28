/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.kirjaswappi.backend.jpa.daos.GenreDao;
import com.kirjaswappi.backend.jpa.repositories.GenreRepository;

/**
 * Integration test for the Genre API endpoint. Tests the complete GET /genres
 * endpoint with real database operations to verify nested response format, HTTP
 * status codes, response headers, and behavior with various database states.
 */
@SpringBootTest
@ActiveProfiles("test")
class GenreApiIntegrationTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private GenreRepository genreRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Clean up existing data
    genreRepository.deleteAll();
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
    return genreRepository.save(genre);
  }
}
