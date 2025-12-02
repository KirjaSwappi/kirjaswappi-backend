/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.utils.LinkBuilder;
import com.kirjaswappi.backend.http.dtos.requests.BookLocationRequest;
import com.kirjaswappi.backend.http.dtos.requests.CreateBookRequest;
import com.kirjaswappi.backend.http.dtos.requests.SwapConditionRequest;
import com.kirjaswappi.backend.http.dtos.requests.UpdateBookRequest;
import com.kirjaswappi.backend.http.dtos.responses.BookListResponse;
import com.kirjaswappi.backend.http.dtos.responses.BookResponse;
import com.kirjaswappi.backend.service.BookService;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;
import com.kirjaswappi.backend.service.enums.SwapType;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.filters.FindAllBooksFilter;

@RestController
@RequestMapping(API_BASE + BOOKS)
@Validated
public class BookController {
  @Autowired
  private BookService bookService;

  @PostMapping(consumes = "multipart/form-data")
  @Operation(summary = "Add book to a user.", responses = {
      @ApiResponse(responseCode = "201", description = "Book created.") })
  public ResponseEntity<BookResponse> createBook(@Valid @ModelAttribute CreateBookRequest book) {
    Book entity = book.toEntity();
    entity = this.parseBookSwapCondition(book.getSwapCondition(), entity);
    entity = this.parseBookLocation(book.getLocation(), entity);
    Book savedBook = bookService.createBook(entity);
    return ResponseEntity.status(HttpStatus.CREATED).body(new BookResponse(savedBook));
  }

  @GetMapping(ID)
  @Operation(summary = "Find book by Book ID.", responses = {
      @ApiResponse(responseCode = "200", description = "Book found.") })
  public ResponseEntity<BookResponse> findBookById(@Parameter(description = "Book ID.") @PathVariable String id) {
    Book book = bookService.getBookById(id);
    return ResponseEntity.status(HttpStatus.OK).body(new BookResponse(book));
  }

  @GetMapping(ID + MORE_BOOKS)
  @Operation(summary = "Find more books of the user by Book ID.", responses = {
      @ApiResponse(responseCode = "200", description = "Books found.") })
  public ResponseEntity<List<BookResponse>> findMoreBooksOfTheUser(
      @Parameter(description = "Book ID.") @PathVariable String id) {
    List<Book> moreBooks = bookService.getMoreBooksOfTheUser(id);
    return ResponseEntity.status(HttpStatus.OK).body(moreBooks.stream().map(BookResponse::new).toList());
  }

  @GetMapping(SUPPORTED_LANGUAGES)
  @Operation(summary = "Find supported book languages.", responses = {
      @ApiResponse(responseCode = "200", description = "List of supported languages.") })
  public ResponseEntity<List<String>> findAllSupportedLanguages() {
    return ResponseEntity.status(HttpStatus.OK).body(Language.getSupportedLanguages());
  }

  @GetMapping(SUPPORTED_CONDITIONS)
  @Operation(summary = "Find supported book conditions.", responses = {
      @ApiResponse(responseCode = "200", description = "List of supported conditions.") })
  public ResponseEntity<List<String>> findAllSupportedConditions() {
    return ResponseEntity.status(HttpStatus.OK).body(Condition.getSupportedConditions());
  }

  @GetMapping(SUPPORTED_SWAP_TYPES)
  @Operation(summary = "Find supported book swap types.", responses = {
      @ApiResponse(responseCode = "200", description = "List of supported swap types.") })
  public ResponseEntity<List<String>> findAllSupportedSwapConditions() {
    return ResponseEntity.status(HttpStatus.OK).body(SwapType.getSupportedSwapTypes());
  }

  @GetMapping
  @Operation(summary = "Search for books with (optional) filter properties, including optional userId and location filters.", responses = {
      @ApiResponse(responseCode = "200", description = "List of Books.") })
  public ResponseEntity<PagedModel<BookListResponse>> findAllBooks(
      @Valid @ParameterObject FindAllBooksFilter filter,
      @PageableDefault() Pageable pageable) {
    Page<Book> books = bookService.getAllBooksByFilter(filter, pageable);
    Page<BookListResponse> response = books.map(BookListResponse::new);
    return ResponseEntity.status(HttpStatus.OK).body(LinkBuilder.forPage(response, API_BASE + BOOKS));
  }

  @GetMapping("/near")
  @Operation(summary = "Find books near a specific location within a given radius.", responses = {
      @ApiResponse(responseCode = "200", description = "List of Books near the specified location."),
      @ApiResponse(responseCode = "400", description = "Invalid coordinates provided.") })
  public ResponseEntity<PagedModel<BookListResponse>> findBooksNearLocation(
      @Parameter(description = "Latitude coordinate (-85 to 85 degrees)") Double latitude,
      @Parameter(description = "Longitude coordinate (-180 to 180 degrees)") Double longitude,
      @Parameter(description = "Search radius in kilometers (default: 50, max: 1000)") Integer radiusKm,
      @PageableDefault() Pageable pageable) {

    // Validate coordinates
    if (latitude == null || longitude == null) {
      throw new BadRequestException("Both latitude and longitude are required for location search");
    }
    if (!com.kirjaswappi.backend.service.entities.BookLocation.isValidLatitude(latitude)) {
      throw new BadRequestException("Invalid latitude: " + latitude + ". Must be between -85 and 85 degrees.");
    }
    if (!com.kirjaswappi.backend.service.entities.BookLocation.isValidLongitude(longitude)) {
      throw new BadRequestException("Invalid longitude: " + longitude + ". Must be between -180 and 180 degrees.");
    }

    // Validate and cap radius
    int validRadiusKm = radiusKm != null ? Math.min(Math.max(radiusKm, 1), 1000) : 50;

    Page<Book> books = bookService.findBooksNearLocation(latitude, longitude, validRadiusKm, pageable);
    Page<BookListResponse> response = books.map(BookListResponse::new);
    return ResponseEntity.status(HttpStatus.OK).body(LinkBuilder.forPage(response, API_BASE + BOOKS + "/near"));
  }

  @GetMapping("/city/{city}")
  @Operation(summary = "Find books in a specific city.", responses = {
      @ApiResponse(responseCode = "200", description = "List of Books in the specified city.") })
  public ResponseEntity<PagedModel<BookListResponse>> findBooksInCity(
      @Parameter(description = "City name") @PathVariable String city,
      @PageableDefault() Pageable pageable) {
    Page<Book> books = bookService.findBooksInCity(city, pageable);
    Page<BookListResponse> response = books.map(BookListResponse::new);
    return ResponseEntity.status(HttpStatus.OK).body(LinkBuilder.forPage(response, API_BASE + BOOKS + "/city/" + city));
  }

  @GetMapping("/country/{country}")
  @Operation(summary = "Find books in a specific country.", responses = {
      @ApiResponse(responseCode = "200", description = "List of Books in the specified country.") })
  public ResponseEntity<PagedModel<BookListResponse>> findBooksInCountry(
      @Parameter(description = "Country name") @PathVariable String country,
      @PageableDefault() Pageable pageable) {
    Page<Book> books = bookService.findBooksInCountry(country, pageable);
    Page<BookListResponse> response = books.map(BookListResponse::new);
    return ResponseEntity.status(HttpStatus.OK)
        .body(LinkBuilder.forPage(response, API_BASE + BOOKS + "/country/" + country));
  }

  @PutMapping(value = ID, consumes = "multipart/form-data")
  @Operation(summary = "Update book by Book ID.", responses = {
      @ApiResponse(responseCode = "200", description = "Book updated.") })
  public ResponseEntity<BookResponse> updateBook(@Parameter(description = "Book ID.") @PathVariable String id,
      @Valid @ModelAttribute UpdateBookRequest request) {
    // validate id:
    if (!id.equals(request.getId())) {
      throw new BadRequestException("idMismatch", id, request.getId());
    }
    Book entity = request.toEntity();
    entity = this.parseBookSwapCondition(request.getSwapCondition(), entity);
    entity = this.parseBookLocation(request.getLocation(), entity);
    Book updatedBook = bookService.updateBook(entity);
    return ResponseEntity.status(HttpStatus.OK).body(new BookResponse(updatedBook));
  }

  @DeleteMapping(ID)
  @Operation(summary = "Delete book by Book ID.", responses = {
      @ApiResponse(responseCode = "204", description = "Book deleted.") })
  public ResponseEntity<Void> deleteBook(@Parameter(description = "Book ID.") @PathVariable String id) {
    bookService.deleteBook(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  @Operation(summary = "Delete all books.", responses = {
      @ApiResponse(responseCode = "204", description = "All books are deleted.") })
  public ResponseEntity<Void> deleteAllBooks() {
    bookService.deleteAllBooks();
    return ResponseEntity.noContent().build();
  }

  private Book parseBookSwapCondition(String swapConditionJson, Book book) {
    var objectMapper = new ObjectMapper();
    try {
      var swapCondition = objectMapper.readValue(swapConditionJson, SwapConditionRequest.class).toEntity();
      return book.withSwapCondition(swapCondition);
    } catch (Exception _) {
      throw new BadRequestException("invalidSwapConditionRequest", swapConditionJson);
    }
  }

  private Book parseBookLocation(String locationJson, Book book) {
    if (locationJson == null || locationJson.isBlank()) {
      return book;
    }
    var objectMapper = new ObjectMapper();
    try {
      var location = objectMapper.readValue(locationJson, BookLocationRequest.class).toEntity();
      return book.withLocation(location);
    } catch (Exception _) {
      throw new BadRequestException("invalidLocationRequest", locationJson);
    }
  }
}
