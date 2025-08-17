/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.InboxController;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@WebMvcTest(InboxController.class)
@Import(CustomMockMvcConfiguration.class)
class InboxControllerTest {
  private static final String API_PATH = "/api/v1/inbox";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private InboxService inboxService;

  @Test
  @DisplayName("Should return received swap requests successfully")
  void shouldReturnReceivedSwapRequestsSuccessfully() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    User receiver = new User();
    receiver.setId("receiver123");
    receiver.setFirstName("Jane");
    receiver.setLastName("Smith");

    Book book = new Book();
    book.setId("book123");
    book.setTitle("Test Book");
    book.setAuthor("Test Author");
    book.setCondition(Condition.GOOD);

    SwapRequest swapRequest1 = new SwapRequest();
    swapRequest1.setId("swap1");
    swapRequest1.setSender(sender);
    swapRequest1.setReceiver(receiver);
    swapRequest1.setBookToSwapWith(book);
    swapRequest1.setSwapType(SwapType.BY_BOOKS);
    swapRequest1.setSwapStatus(SwapStatus.PENDING);
    swapRequest1.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest1.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest1.setAskForGiveaway(false);

    SwapRequest swapRequest2 = new SwapRequest();
    swapRequest2.setId("swap2");
    swapRequest2.setSender(sender);
    swapRequest2.setReceiver(receiver);
    swapRequest2.setBookToSwapWith(book);
    swapRequest2.setSwapType(SwapType.GIVE_AWAY);
    swapRequest2.setSwapStatus(SwapStatus.ACCEPTED);
    swapRequest2.setRequestedAt(Instant.parse("2025-01-02T10:00:00Z"));
    swapRequest2.setUpdatedAt(Instant.parse("2025-01-02T10:00:00Z"));
    swapRequest2.setAskForGiveaway(true);

    List<SwapRequest> swapRequests = Arrays.asList(swapRequest1, swapRequest2);
    when(inboxService.getReceivedSwapRequests("receiver123", null, null)).thenReturn(swapRequests);
    when(inboxService.getUnreadMessageCount("receiver123", "swap1")).thenReturn(2L);
    when(inboxService.getUnreadMessageCount("receiver123", "swap2")).thenReturn(0L);

    // When & Then
    mockMvc.perform(get(API_PATH + "/received")
        .param("userId", "receiver123"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("swap1"))
        .andExpect(jsonPath("$[0].swapType").value("ByBooks"))
        .andExpect(jsonPath("$[0].swapStatus").value("Pending"))
        .andExpect(jsonPath("$[0].askForGiveaway").value(false))
        .andExpect(jsonPath("$[0].unreadMessageCount").value(2))
        .andExpect(jsonPath("$[0].sender.id").value("sender123"))
        .andExpect(jsonPath("$[0].sender.name").value("John Doe"))
        .andExpect(jsonPath("$[0].receiver.id").value("receiver123"))
        .andExpect(jsonPath("$[0].receiver.name").value("Jane Smith"))
        .andExpect(jsonPath("$[0].bookToSwapWith.id").value("book123"))
        .andExpect(jsonPath("$[0].bookToSwapWith.title").value("Test Book"))
        .andExpect(jsonPath("$[0].bookToSwapWith.condition").value("Good"))
        .andExpect(jsonPath("$[1].id").value("swap2"))
        .andExpect(jsonPath("$[1].swapType").value("GiveAway"))
        .andExpect(jsonPath("$[1].swapStatus").value("Accepted"))
        .andExpect(jsonPath("$[1].askForGiveaway").value(true))
        .andExpect(jsonPath("$[1].unreadMessageCount").value(0));

    verify(inboxService).getReceivedSwapRequests("receiver123", null, null);
    verify(inboxService).getUnreadMessageCount("receiver123", "swap1");
    verify(inboxService).getUnreadMessageCount("receiver123", "swap2");
  }

  @Test
  @DisplayName("Should return received swap requests with status filter")
  void shouldReturnReceivedSwapRequestsWithStatusFilter() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    User receiver = new User();
    receiver.setId("receiver123");
    receiver.setFirstName("Jane");
    receiver.setLastName("Smith");

    Book book = new Book();
    book.setId("book123");
    book.setTitle("Test Book");
    book.setAuthor("Test Author");
    book.setCondition(Condition.GOOD);

    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap1");
    swapRequest.setSender(sender);
    swapRequest.setReceiver(receiver);
    swapRequest.setBookToSwapWith(book);
    swapRequest.setSwapType(SwapType.BY_BOOKS);
    swapRequest.setSwapStatus(SwapStatus.PENDING);
    swapRequest.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest.setAskForGiveaway(false);

    List<SwapRequest> swapRequests = Arrays.asList(swapRequest);
    when(inboxService.getReceivedSwapRequests("receiver123", "Pending", "date")).thenReturn(swapRequests);
    when(inboxService.getUnreadMessageCount("receiver123", "swap1")).thenReturn(1L);

    // When & Then
    mockMvc.perform(get(API_PATH + "/received")
        .param("userId", "receiver123")
        .param("status", "Pending")
        .param("sortBy", "date"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("swap1"))
        .andExpect(jsonPath("$[0].swapStatus").value("Pending"))
        .andExpect(jsonPath("$[0].unreadMessageCount").value(1));

    verify(inboxService).getReceivedSwapRequests("receiver123", "Pending", "date");
    verify(inboxService).getUnreadMessageCount("receiver123", "swap1");
  }

  @Test
  @DisplayName("Should return sent swap requests successfully")
  void shouldReturnSentSwapRequestsSuccessfully() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    User receiver = new User();
    receiver.setId("receiver123");
    receiver.setFirstName("Jane");
    receiver.setLastName("Smith");

    Book book = new Book();
    book.setId("book123");
    book.setTitle("Test Book");
    book.setAuthor("Test Author");
    book.setCondition(Condition.LIKE_NEW);

    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap1");
    swapRequest.setSender(sender);
    swapRequest.setReceiver(receiver);
    swapRequest.setBookToSwapWith(book);
    swapRequest.setSwapType(SwapType.OPEN_FOR_OFFERS);
    swapRequest.setSwapStatus(SwapStatus.ACCEPTED);
    swapRequest.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest.setAskForGiveaway(false);

    List<SwapRequest> swapRequests = Arrays.asList(swapRequest);
    when(inboxService.getSentSwapRequests("sender123", null, null)).thenReturn(swapRequests);
    when(inboxService.getUnreadMessageCount("sender123", "swap1")).thenReturn(3L);

    // When & Then
    mockMvc.perform(get(API_PATH + "/sent")
        .param("userId", "sender123"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("swap1"))
        .andExpect(jsonPath("$[0].swapType").value("OpenForOffers"))
        .andExpect(jsonPath("$[0].swapStatus").value("Accepted"))
        .andExpect(jsonPath("$[0].unreadMessageCount").value(3))
        .andExpect(jsonPath("$[0].bookToSwapWith.condition").value("Like New"));

    verify(inboxService).getSentSwapRequests("sender123", null, null);
    verify(inboxService).getUnreadMessageCount("sender123", "swap1");
  }

  @Test
  @DisplayName("Should return 400 when invalid status provided")
  void shouldReturn400WhenInvalidStatusProvided() throws Exception {
    // Given
    when(inboxService.getReceivedSwapRequests("receiver123", "InvalidStatus", null))
        .thenThrow(new BadRequestException("invalidSwapStatus", "InvalidStatus"));

    // When & Then
    mockMvc.perform(get(API_PATH + "/received")
        .param("userId", "receiver123")
        .param("status", "InvalidStatus"))
        .andExpect(status().isBadRequest());

    verify(inboxService).getReceivedSwapRequests("receiver123", "InvalidStatus", null);
    verifyNoMoreInteractions(inboxService);
  }

  @Test
  @DisplayName("Should return 404 when user not found")
  void shouldReturn404WhenUserNotFound() throws Exception {
    // Given
    when(inboxService.getReceivedSwapRequests("nonexistent", null, null))
        .thenThrow(new UserNotFoundException());

    // When & Then
    mockMvc.perform(get(API_PATH + "/received")
        .param("userId", "nonexistent"))
        .andExpect(status().isNotFound());

    verify(inboxService).getReceivedSwapRequests("nonexistent", null, null);
    verifyNoMoreInteractions(inboxService);
  }

  @Test
  @DisplayName("Should return sent swap requests with sorting")
  void shouldReturnSentSwapRequestsWithSorting() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    User receiver = new User();
    receiver.setId("receiver123");
    receiver.setFirstName("Jane");
    receiver.setLastName("Smith");

    Book book = new Book();
    book.setId("book123");
    book.setTitle("Test Book");
    book.setAuthor("Test Author");

    SwapRequest swapRequest = new SwapRequest();
    swapRequest.setId("swap1");
    swapRequest.setSender(sender);
    swapRequest.setReceiver(receiver);
    swapRequest.setBookToSwapWith(book);
    swapRequest.setSwapType(SwapType.BY_GENRES);
    swapRequest.setSwapStatus(SwapStatus.REJECTED);
    swapRequest.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest.setAskForGiveaway(false);

    List<SwapRequest> swapRequests = Arrays.asList(swapRequest);
    when(inboxService.getSentSwapRequests("sender123", "Rejected", "book_title")).thenReturn(swapRequests);
    when(inboxService.getUnreadMessageCount("sender123", "swap1")).thenReturn(0L);

    // When & Then
    mockMvc.perform(get(API_PATH + "/sent")
        .param("userId", "sender123")
        .param("status", "Rejected")
        .param("sortBy", "book_title"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("swap1"))
        .andExpect(jsonPath("$[0].swapType").value("ByGenres"))
        .andExpect(jsonPath("$[0].swapStatus").value("Rejected"))
        .andExpect(jsonPath("$[0].unreadMessageCount").value(0));

    verify(inboxService).getSentSwapRequests("sender123", "Rejected", "book_title");
    verify(inboxService).getUnreadMessageCount("sender123", "swap1");
  }
}