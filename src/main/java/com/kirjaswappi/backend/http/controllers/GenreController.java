/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.API_BASE;
import static com.kirjaswappi.backend.common.utils.Constants.GENRES;
import static com.kirjaswappi.backend.common.utils.Constants.ID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kirjaswappi.backend.http.dtos.requests.CreateGenreRequest;
import com.kirjaswappi.backend.http.dtos.requests.UpdateGenreRequest;
import com.kirjaswappi.backend.http.dtos.responses.GenreResponse;
import com.kirjaswappi.backend.http.dtos.responses.NestedGenresResponse;
import com.kirjaswappi.backend.service.GenreService;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@RestController
@RequestMapping(API_BASE + GENRES)
@Validated
public class GenreController {
  @Autowired
  private GenreService genreService;

  @GetMapping
  @Operation(summary = "Find all genres in nested hierarchical format.", description = "Returns genres organized in a nested structure where parent genres contain their child genres. Parent genres are returned as top-level keys with their children nested inside.", responses = {
      @ApiResponse(responseCode = "200", description = "Nested structure of genres with parent genres as keys and child genres nested inside.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NestedGenresResponse.class), examples = @ExampleObject(name = "Nested Genres Example", value = """
          {
            "parentGenres": {
              "Fiction": {
                "id": "1",
                "name": "Fiction",
                "childGenres": [
                  {
                    "id": "2",
                    "name": "Science Fiction"
                  },
                  {
                    "id": "3",
                    "name": "Fantasy"
                  }
                ]
              },
              "Non-Fiction": {
                "id": "4",
                "name": "Non-Fiction",
                "childGenres": [
                  {
                    "id": "5",
                    "name": "Biography"
                  }
                ]
              }
            }
          }
          """)))
  })
  public ResponseEntity<NestedGenresResponse> findGenres() {
    NestedGenresResponse nestedGenresResponse = genreService.getNestedGenres();
    return ResponseEntity.status(HttpStatus.OK).body(nestedGenresResponse);
  }

  @PostMapping
  @Operation(summary = "Create genre.", responses = {
      @ApiResponse(responseCode = "201", description = "Genre created.") })
  public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody CreateGenreRequest request) {
    Genre savedGenre = genreService.addGenre(request.toEntity());
    return ResponseEntity.status(HttpStatus.CREATED).body(new GenreResponse(savedGenre));
  }

  @PutMapping(ID)
  @Operation(summary = "Update genre.", responses = {
      @ApiResponse(responseCode = "200", description = "Genre updated.") })
  public ResponseEntity<GenreResponse> updateGenre(@Parameter(description = "Genre ID.") @PathVariable String id,
      @Valid @RequestBody UpdateGenreRequest request) {
    // validate id:
    if (!id.equals(request.getId())) {
      throw new BadRequestException("idMismatch", id, request.getId());
    }
    Genre updatedGenre = genreService.updateGenre(request.toEntity());
    return ResponseEntity.status(HttpStatus.OK).body(new GenreResponse(updatedGenre));
  }

  @DeleteMapping(ID)
  @Operation(summary = "Delete genre.", responses = {
      @ApiResponse(responseCode = "204", description = "Genre deleted.") })
  public ResponseEntity<Void> deleteGenre(@Parameter(description = "Genre ID.") @PathVariable String id) {
    genreService.deleteGenre(id);
    return ResponseEntity.noContent().build();
  }
}
