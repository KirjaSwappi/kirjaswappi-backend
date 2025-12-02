/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.SwapController;
import com.kirjaswappi.backend.http.dtos.requests.CreateSwapRequest;
import com.kirjaswappi.backend.http.dtos.requests.UpdateSwapStatusRequest;
import com.kirjaswappi.backend.service.SwapService;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.*;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.InvalidStatusTransitionException;
import com.kirjaswappi.backend.service.exceptions.SwapRequestNotFoundException;

/**
 * Unit tests for Swap Request API endpoints using WebMvcTest with mocked
 * services. Tests the complete swap request lifecycle including creation,
 * status updates, and deletion.
 */
@WebMvcTest(SwapController.class)
@Import(CustomMockMvcConfiguration.class)
class SwapRequestApiIntegrationTest {

  private static final String API_BASE = "/api/v1/swap-requests";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SwapService swapService;

  @Autowired
  private ObjectMapper objectMapper;

  private User createTestUser(String id, String firstName) {
    return User.builder()
        .id(id)
        .firstName(firstName)
        .lastName("User")
        .email(firstName.toLowerCase() + "@example.com")
        .build();
  }

  private Book createTestBook(String id, String title, User owner) {
    return Book.builder()
        .id(id)
        .title(title)
        .author("Author")
        .condition(Condition.GOOD)
        .language(Language.ENGLISH)
        .owner(owner)
        .genres(List.of())
        .swapCondition(SwapCondition.builder()
            .swapType(SwapType.BY_BOOKS)
            .giveAway(false)
            .build())
        .build();
  }

  private SwapRequest createTestSwapRequest(String id, User sender, User receiver, Book book, SwapStatus status) {
    return SwapRequest.builder()
        .id(id)
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.BY_BOOKS)
        .swapStatus(status)
        .askForGiveaway(false)
        .note("Test swap request")
        .requestedAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Nested
  @DisplayName("Create Swap Request Tests")
  class CreateSwapRequestTests {

    @Test
    @DisplayName("Should create swap request with offered book successfully")
    void shouldCreateSwapRequestWithOfferedBook() throws Exception {
      User sender = createTestUser("sender-1", "Sender");
      User receiver = createTestUser("receiver-1", "Receiver");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest("swap-1", sender, receiver, book, SwapStatus.PENDING);

      when(swapService.createSwapRequest(any())).thenReturn(swapRequest);

      CreateSwapRequest request = new CreateSwapRequest();
      request.setSenderId("sender-1");
      request.setReceiverId("receiver-1");
      request.setBookIdToSwapWith("book-1");
      request.setSwapType("ByBooks");
      request.setAskForGiveaway(false);
      request.setNote("I'd like to swap!");

      CreateSwapRequest.SwapOfferRequest offer = new CreateSwapRequest.SwapOfferRequest();
      offer.setOfferedBookId("offered-book-1");
      request.setSwapOffer(offer);

      mockMvc.perform(post(API_BASE)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should create swap request with offered genre successfully")
    void shouldCreateSwapRequestWithOfferedGenre() throws Exception {
      User sender = createTestUser("sender-1", "Sender");
      User receiver = createTestUser("receiver-1", "Receiver");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest("swap-1", sender, receiver, book, SwapStatus.PENDING);

      when(swapService.createSwapRequest(any())).thenReturn(swapRequest);

      CreateSwapRequest request = new CreateSwapRequest();
      request.setSenderId("sender-1");
      request.setReceiverId("receiver-1");
      request.setBookIdToSwapWith("book-1");
      request.setSwapType("ByGenres");
      request.setAskForGiveaway(false);

      CreateSwapRequest.SwapOfferRequest offer = new CreateSwapRequest.SwapOfferRequest();
      offer.setOfferedGenreId("genre-1");
      request.setSwapOffer(offer);

      mockMvc.perform(post(API_BASE)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should create giveaway swap request successfully")
    void shouldCreateGiveawaySwapRequest() throws Exception {
      User sender = createTestUser("sender-1", "Sender");
      User receiver = createTestUser("receiver-1", "Receiver");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest("swap-1", sender, receiver, book, SwapStatus.PENDING);

      when(swapService.createSwapRequest(any())).thenReturn(swapRequest);

      CreateSwapRequest request = new CreateSwapRequest();
      request.setSenderId("sender-1");
      request.setReceiverId("receiver-1");
      request.setBookIdToSwapWith("book-1");
      request.setSwapType("GiveAway");
      request.setAskForGiveaway(true);

      CreateSwapRequest.SwapOfferRequest offer = new CreateSwapRequest.SwapOfferRequest();
      offer.setOfferedBookId("offered-book-1");
      request.setSwapOffer(offer);

      mockMvc.perform(post(API_BASE)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 when required fields are missing")
    void shouldReturn400WhenFieldsMissing() throws Exception {
      CreateSwapRequest request = new CreateSwapRequest();
      // Missing all required fields

      mockMvc.perform(post(API_BASE)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when both book and genre are offered")
    void shouldReturn400WhenBothOffered() throws Exception {
      CreateSwapRequest request = new CreateSwapRequest();
      request.setSenderId("sender-1");
      request.setReceiverId("receiver-1");
      request.setBookIdToSwapWith("book-1");
      request.setSwapType("ByBooks");

      CreateSwapRequest.SwapOfferRequest offer = new CreateSwapRequest.SwapOfferRequest();
      offer.setOfferedBookId("offered-book-1");
      offer.setOfferedGenreId("genre-1"); // Both set
      request.setSwapOffer(offer);

      mockMvc.perform(post(API_BASE)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when no swap offer is provided")
    void shouldReturn400WhenNoOffer() throws Exception {
      CreateSwapRequest request = new CreateSwapRequest();
      request.setSenderId("sender-1");
      request.setReceiverId("receiver-1");
      request.setBookIdToSwapWith("book-1");
      request.setSwapType("ByBooks");
      request.setSwapOffer(new CreateSwapRequest.SwapOfferRequest()); // Empty offer

      mockMvc.perform(post(API_BASE)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Update Swap Status Tests")
  class UpdateSwapStatusTests {

    @Test
    @DisplayName("Should update status to Accepted successfully")
    void shouldUpdateStatusToAccepted() throws Exception {
      User sender = createTestUser("sender-1", "Sender");
      User receiver = createTestUser("receiver-1", "Receiver");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest("swap-1", sender, receiver, book, SwapStatus.ACCEPTED);

      when(swapService.updateSwapRequestStatus(eq("swap-1"), eq(SwapStatus.ACCEPTED), eq("receiver-1")))
          .thenReturn(swapRequest);

      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("Accepted");

      mockMvc.perform(put(API_BASE + "/swap-1/status")
          .header("X-User-Id", "receiver-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should update status to Rejected successfully")
    void shouldUpdateStatusToRejected() throws Exception {
      User sender = createTestUser("sender-1", "Sender");
      User receiver = createTestUser("receiver-1", "Receiver");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest("swap-1", sender, receiver, book, SwapStatus.REJECTED);

      when(swapService.updateSwapRequestStatus(eq("swap-1"), eq(SwapStatus.REJECTED), eq("receiver-1")))
          .thenReturn(swapRequest);

      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("Rejected");

      mockMvc.perform(put(API_BASE + "/swap-1/status")
          .header("X-User-Id", "receiver-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should update status to Reserved successfully")
    void shouldUpdateStatusToCompleted() throws Exception {
      User sender = createTestUser("sender-1", "Sender");
      User receiver = createTestUser("receiver-1", "Receiver");
      Book book = createTestBook("book-1", "Test Book", receiver);
      SwapRequest swapRequest = createTestSwapRequest("swap-1", sender, receiver, book, SwapStatus.RESERVED);

      when(swapService.updateSwapRequestStatus(eq("swap-1"), eq(SwapStatus.RESERVED), eq("receiver-1")))
          .thenReturn(swapRequest);

      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("Reserved");

      mockMvc.perform(put(API_BASE + "/swap-1/status")
          .header("X-User-Id", "receiver-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 when invalid status provided")
    void shouldReturn400WhenInvalidStatus() throws Exception {
      // Invalid status will throw BadRequestException before service is called
      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("InvalidStatus");

      mockMvc.perform(put(API_BASE + "/swap-1/status")
          .header("X-User-Id", "receiver-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when status is blank")
    void shouldReturn400WhenStatusBlank() throws Exception {
      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("");

      mockMvc.perform(put(API_BASE + "/swap-1/status")
          .header("X-User-Id", "receiver-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    void shouldReturn400WhenUserHeaderMissing() throws Exception {
      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("Accepted");

      mockMvc.perform(put(API_BASE + "/swap-1/status")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when swap request not found")
    void shouldReturn404WhenSwapRequestNotFound() throws Exception {
      when(swapService.updateSwapRequestStatus(eq("nonexistent-id"), eq(SwapStatus.ACCEPTED), eq("receiver-1")))
          .thenThrow(new SwapRequestNotFoundException("nonexistent-id"));

      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("Accepted");

      mockMvc.perform(put(API_BASE + "/nonexistent-id/status")
          .header("X-User-Id", "receiver-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when sender tries to update status")
    void shouldReturn400WhenSenderTriesToUpdate() throws Exception {
      when(swapService.updateSwapRequestStatus(eq("swap-1"), eq(SwapStatus.ACCEPTED), eq("sender-1")))
          .thenThrow(new BadRequestException("unauthorizedUpdate", "sender-1"));

      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("Accepted");

      mockMvc.perform(put(API_BASE + "/swap-1/status")
          .header("X-User-Id", "sender-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for invalid status transition")
    void shouldReturn400ForInvalidTransition() throws Exception {
      when(swapService.updateSwapRequestStatus(eq("swap-1"), eq(SwapStatus.PENDING), eq("receiver-1")))
          .thenThrow(new InvalidStatusTransitionException("Reserved", "Pending"));

      UpdateSwapStatusRequest request = new UpdateSwapStatusRequest();
      request.setStatus("Pending");

      mockMvc.perform(put(API_BASE + "/swap-1/status")
          .header("X-User-Id", "receiver-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Delete Swap Request Tests")
  class DeleteSwapRequestTests {

    @Test
    @DisplayName("Should delete all swap requests successfully")
    void shouldDeleteAllSwapRequestsSuccessfully() throws Exception {
      doNothing().when(swapService).deleteAllSwapRequests();

      mockMvc.perform(delete(API_BASE))
          .andExpect(status().isNoContent());
    }
  }
}
