/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.SwapController;
import com.kirjaswappi.backend.http.dtos.requests.CreateSwapRequest;
import com.kirjaswappi.backend.http.dtos.requests.UpdateSwapStatusRequest;
import com.kirjaswappi.backend.service.SwapService;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;
import com.kirjaswappi.backend.service.exceptions.InvalidStatusTransitionException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

@WebMvcTest(SwapController.class)
@Import(CustomMockMvcConfiguration.class)
class SwapControllerTest {
  private static final String API_PATH = "/api/v1/swap-requests";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SwapService swapService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should return OK when creating a valid swap request")
  void shouldReturnOk_whenValidRequest() throws Exception {
    var request = new CreateSwapRequest();
    request.setSenderId("user1");
    request.setReceiverId("user2");
    request.setBookIdToSwapWith("book1");
    request.setSwapType("ByBooks");
    request.setAskForGiveaway(true);
    request.setNote("I'd like to swap");

    CreateSwapRequest.SwapOfferRequest offer = new CreateSwapRequest.SwapOfferRequest();
    offer.setOfferedBookId("book2");
    request.setSwapOffer(offer);

    // Setup mocked SwapRequest entity

    User sender = User.builder()
        .id("user1")
        .build();

    User receiver = User.builder()
        .id("user2")
        .build();

    Book bookToSwapWith = Book.builder()
        .id("book1")
        .title("Book One")
        .author("Author A")
        .build();

    SwappableBook offeredBook = SwappableBook.builder()
        .id("book2")
        .title("Offered Book")
        .author("Author B")
        .coverPhoto("https://example.com/cover2.jpg")
        .build();
    SwapOffer offerEntity = SwapOffer.builder().offeredBook(offeredBook)
        .offeredGenre(null)
        .build();

    SwapRequest entity = SwapRequest.builder()
        .id("swap-001")
        .bookToSwapWith(bookToSwapWith)
        .sender(sender)
        .receiver(receiver)
        .swapOffer(offerEntity)
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(SwapStatus.PENDING)
        .askForGiveaway(false)
        .note("I'd like to swap")
        .requestedAt(Instant.parse("2024-01-01T00:00:00Z"))
        .updatedAt(Instant.parse("2024-01-01T01:00:00Z"))
        .build();

    when(swapService.createSwapRequest(any())).thenReturn(entity);

    mockMvc.perform(post(API_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("swap-001"))
        .andExpect(jsonPath("$.senderId").value("user1"))
        .andExpect(jsonPath("$.receiverId").value("user2"))
        .andExpect(jsonPath("$.bookToSwapWith.id").value("book1"))
        .andExpect(jsonPath("$.bookToSwapWith.title").value("Book One"))
        .andExpect(jsonPath("$.swapType").value("ByBooks"))
        .andExpect(jsonPath("$.swapOffer.offeredBook.id").value("book2"))
        .andExpect(jsonPath("$.swapOffer.offeredBook.title").value("Offered Book"))
        .andExpect(jsonPath("$.askForGiveaway").value(false))
        .andExpect(jsonPath("$.swapStatus").value("Pending"))
        .andExpect(jsonPath("$.note").value("I'd like to swap"))
        .andExpect(jsonPath("$.requestedAt").value("2024-01-01T00:00:00Z"))
        .andExpect(jsonPath("$.updatedAt").value("2024-01-01T01:00:00Z"));
  }

  @Test
  @DisplayName("Should return BadRequest when required fields are missing")
  void shouldReturnBadRequest_whenMissingRequiredFields() throws Exception {
    CreateSwapRequest request = new CreateSwapRequest(); // All fields null

    mockMvc.perform(post(API_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return BadRequest when both swap offers are present")
  void shouldReturnBadRequest_whenBothSwapOffersPresent() throws Exception {
    CreateSwapRequest request = new CreateSwapRequest();
    request.setSenderId("user1");
    request.setReceiverId("user2");
    request.setBookIdToSwapWith("book1");
    request.setSwapType("ByGenres");

    CreateSwapRequest.SwapOfferRequest offer = new CreateSwapRequest.SwapOfferRequest();
    offer.setOfferedBookId("book2");
    offer.setOfferedGenreId("genre1");
    request.setSwapOffer(offer);

    mockMvc.perform(post(API_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return BadRequest when no swap offer is present")
  void shouldReturnBadRequest_whenNoSwapOfferPresent() throws Exception {
    CreateSwapRequest request = new CreateSwapRequest();
    request.setSenderId("user1");
    request.setReceiverId("user2");
    request.setBookIdToSwapWith("book1");
    request.setSwapType("ByGenres");

    request.setSwapOffer(new CreateSwapRequest.SwapOfferRequest()); // both null

    mockMvc.perform(post(API_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return NoContent when deleting all swap requests")
  void shouldReturnNoContent() throws Exception {
    mockMvc.perform(delete(API_PATH))
        .andExpect(status().isNoContent());

    verify(swapService, times(1)).deleteAllSwapRequests();
  }

  @Test
  @DisplayName("Should return OK when updating swap request status with valid data")
  void shouldReturnOk_whenUpdatingSwapRequestStatus() throws Exception {
    var swapRequestId = "swap-001";
    var userId = "user2";

    var request = new UpdateSwapStatusRequest();
    request.setStatus("Accepted");

    var sender = User.builder()
        .id("user1")
        .build();

    var receiver = User.builder()
        .id(userId)
        .build();

    var bookToSwapWith = Book.builder()
        .id("book1")
        .title("Book One")
        .author("Author A")
        .build();

    var entity = SwapRequest.builder()
        .id(swapRequestId)
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(bookToSwapWith)
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(SwapStatus.ACCEPTED)
        .askForGiveaway(false)
        .note("I'd like to swap")
        .requestedAt(Instant.parse("2024-01-01T00:00:00Z"))
        .updatedAt(Instant.parse("2024-01-01T01:00:00Z"))
        .build();

    when(swapService.updateSwapRequestStatus(eq(swapRequestId), eq(SwapStatus.ACCEPTED), eq(userId)))
        .thenReturn(entity);

    mockMvc.perform(put(API_PATH + "/" + swapRequestId + "/status")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-User-Id", userId)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(swapRequestId))
        .andExpect(jsonPath("$.senderId").value("user1"))
        .andExpect(jsonPath("$.receiverId").value(userId))
        .andExpect(jsonPath("$.swapStatus").value("Accepted"));

    verify(swapService, times(1)).updateSwapRequestStatus(swapRequestId, SwapStatus.ACCEPTED, userId);
  }

  @Test
  @DisplayName("Should return BadRequest when updating with invalid status")
  void shouldReturnBadRequest_whenInvalidStatus() throws Exception {
    String swapRequestId = "swap-001";
    String userId = "user2";
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("InvalidStatus");

    mockMvc.perform(put(API_PATH + "/" + swapRequestId + "/status")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-User-Id", userId)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(swapService, never()).updateSwapRequestStatus(any(), any(), any());
  }

  @Test
  @DisplayName("Should return BadRequest when status is blank")
  void shouldReturnBadRequest_whenStatusIsBlank() throws Exception {
    String swapRequestId = "swap-001";
    String userId = "user2";
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("");

    mockMvc.perform(put(API_PATH + "/" + swapRequestId + "/status")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-User-Id", userId)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(swapService, never()).updateSwapRequestStatus(any(), any(), any());
  }

  @Test
  @DisplayName("Should return NotFound when swap request does not exist")
  void shouldReturnNotFound_whenSwapRequestNotExists() throws Exception {
    String swapRequestId = "nonexistent";
    String userId = "user2";
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("Accepted");

    when(swapService.updateSwapRequestStatus(eq(swapRequestId), eq(SwapStatus.ACCEPTED), eq(userId)))
        .thenThrow(new SwapRequestNotFoundException(swapRequestId));

    mockMvc.perform(put(API_PATH + "/" + swapRequestId + "/status")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-User-Id", userId)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());

    verify(swapService, times(1)).updateSwapRequestStatus(swapRequestId, SwapStatus.ACCEPTED, userId);
  }

  @Test
  @DisplayName("Should return BadRequest when invalid status transition")
  void shouldReturnBadRequest_whenInvalidStatusTransition() throws Exception {
    String swapRequestId = "swap-001";
    String userId = "user2";
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("Pending");

    when(swapService.updateSwapRequestStatus(eq(swapRequestId), eq(SwapStatus.PENDING), eq(userId)))
        .thenThrow(new InvalidStatusTransitionException("Accepted", "Pending"));

    mockMvc.perform(put(API_PATH + "/" + swapRequestId + "/status")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-User-Id", userId)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(swapService, times(1)).updateSwapRequestStatus(swapRequestId, SwapStatus.PENDING, userId);
  }

  @Test
  @DisplayName("Should return BadRequest when missing user header")
  void shouldReturnBadRequest_whenMissingUserHeader() throws Exception {
    String swapRequestId = "swap-001";
    UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
    request.setStatus("Accepted");

    mockMvc.perform(put(API_PATH + "/" + swapRequestId + "/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(swapService, never()).updateSwapRequestStatus(any(), any(), any());
  }
}
