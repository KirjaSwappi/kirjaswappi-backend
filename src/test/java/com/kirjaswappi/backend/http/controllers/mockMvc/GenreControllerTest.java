/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.GenreController;
import com.kirjaswappi.backend.http.dtos.requests.CreateGenreRequest;
import com.kirjaswappi.backend.http.dtos.requests.UpdateGenreRequest;
import com.kirjaswappi.backend.http.dtos.responses.NestedGenresResponse;
import com.kirjaswappi.backend.http.dtos.responses.ParentGenreResponse;
import com.kirjaswappi.backend.service.GenreService;
import com.kirjaswappi.backend.service.entities.Genre;

@WebMvcTest(GenreController.class)
@Import(CustomMockMvcConfiguration.class)
class GenreControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private GenreService genreService;

  private Genre genre;

  @BeforeEach
  void setup() {
    genre = new Genre("1", "Fantasy", null);
  }

  @Test
  @DisplayName("Should return all genres in nested format")
  void shouldReturnGenres() throws Exception {
    // Create a parent genre response with the Fantasy genre
    ParentGenreResponse parentGenreResponse = new ParentGenreResponse(genre, List.of());
    Map<String, ParentGenreResponse> parentGenresMap = new HashMap<>();
    parentGenresMap.put("Fantasy", parentGenreResponse);
    NestedGenresResponse nestedResponse = new NestedGenresResponse(parentGenresMap);

    when(genreService.getNestedGenres()).thenReturn(nestedResponse);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentGenres.Fantasy.id").value("1"))
        .andExpect(jsonPath("$.parentGenres.Fantasy.name").value("Fantasy"))
        .andExpect(jsonPath("$.parentGenres.Fantasy.childGenres").isArray())
        .andExpect(jsonPath("$.parentGenres.Fantasy.childGenres").isEmpty());
  }

  @Test
  @DisplayName("Should return empty nested genre structure")
  void shouldReturnEmptyList() throws Exception {
    NestedGenresResponse emptyResponse = new NestedGenresResponse(new HashMap<>());
    when(genreService.getNestedGenres()).thenReturn(emptyResponse);

    mockMvc.perform(get("/api/v1/genres"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentGenres").isMap())
        .andExpect(jsonPath("$.parentGenres").isEmpty());
  }

  @Test
  @DisplayName("Should create genre successfully")
  void shouldCreateGenre() throws Exception {
    CreateGenreRequest request = new CreateGenreRequest();
    request.setName("Sci-Fi");

    Genre savedGenre = new Genre("2", "Sci-Fi", null);
    when(genreService.addGenre(any())).thenReturn(savedGenre);

    mockMvc.perform(post("/api/v1/genres")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("2"))
        .andExpect(jsonPath("$.name").value("Sci-Fi"));
  }

  @Test
  @DisplayName("Should return 400 for invalid genre creation request")
  void shouldReturn400ForInvalidCreateRequest() throws Exception {
    CreateGenreRequest request = new CreateGenreRequest(); // name is null/invalid

    mockMvc.perform(post("/api/v1/genres")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should update genre successfully")
  void shouldUpdateGenre() throws Exception {
    UpdateGenreRequest request = new UpdateGenreRequest();
    request.setId("1");
    request.setName("Adventure");

    Genre updatedGenre = new Genre("1", "Adventure", null);
    when(genreService.updateGenre(any())).thenReturn(updatedGenre);

    mockMvc.perform(put("/api/v1/genres/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("1"))
        .andExpect(jsonPath("$.name").value("Adventure"));
  }

  @Test
  @DisplayName("Should throw 400 when path ID and body ID mismatch")
  void shouldReturn400OnIdMismatch() throws Exception {
    UpdateGenreRequest request = new UpdateGenreRequest();
    request.setId("999");
    request.setName("Adventure");

    mockMvc.perform(put("/api/v1/genres/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should delete genre successfully")
  void shouldDeleteGenre() throws Exception {
    doNothing().when(genreService).deleteGenre("1");

    mockMvc.perform(delete("/api/v1/genres/1"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should return 400 on invalid update request body")
  void shouldReturn400ForInvalidUpdateRequest() throws Exception {
    UpdateGenreRequest request = new UpdateGenreRequest();
    request.setId("1");
    // no name set

    mockMvc.perform(put("/api/v1/genres/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
