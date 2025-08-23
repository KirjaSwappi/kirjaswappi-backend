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
  @DisplayName("Should return unified inbox successfully")
  void shouldReturnUnifiedInboxSuccessfully() throws Exception {
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

    // Received swap request
    SwapRequest receivedSwap = new SwapRequest();
    receivedSwap.setId("received1");
    receivedSwap.setSender(sender);
    receivedSwap.setReceiver(receiver);
    receivedSwap.setBookToSwapWith(book);
    receivedSwap.setSwapType(SwapType.BY_BOOKS);
    receivedSwap.setSwapStatus(SwapStatus.PENDING);
    receivedSwap.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    receivedSwap.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));
    receivedSwap.setAskForGiveaway(false);

    // Sent swap request
    SwapRequest sentSwap = new SwapRequest();
    sentSwap.setId("sent1");
    sentSwap.setSender(receiver);
    sentSwap.setReceiver(sender);
    sentSwap.setBookToSwapWith(book);
    sentSwap.setSwapType(SwapType.GIVE_AWAY);
    sentSwap.setSwapStatus(SwapStatus.ACCEPTED);
    sentSwap.setRequestedAt(Instant.parse("2025-01-02T10:00:00Z"));
    sentSwap.setUpdatedAt(Instant.parse("2025-01-02T10:00:00Z"));
    sentSwap.setAskForGiveaway(true);

    List<SwapRequest> unifiedInbox = Arrays.asList(sentSwap, receivedSwap); // Sorted by latest messages
    when(inboxService.getUnifiedInbox("receiver123", null, null)).thenReturn(unifiedInbox);
    when(inboxService.getUnreadMessageCount("receiver123", "received1")).thenReturn(2L);
    when(inboxService.getUnreadMessageCount("receiver123", "sent1")).thenReturn(0L);
    when(inboxService.isInboxItemUnread(receivedSwap, "receiver123")).thenReturn(true);
    when(inboxService.isInboxItemUnread(sentSwap, "receiver123")).thenReturn(false);

    // When & Then
    mockMvc.perform(get(API_PATH)
        .param("userId", "receiver123"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("sent1"))
        .andExpect(jsonPath("$[0].conversationType").value("sent"))
        .andExpect(jsonPath("$[0].swapType").value("GiveAway"))
        .andExpect(jsonPath("$[0].swapStatus").value("Accepted"))
        .andExpect(jsonPath("$[0].unreadMessageCount").value(0))
        .andExpect(jsonPath("$[0].hasNewMessages").value(false))
        .andExpect(jsonPath("$[1].id").value("received1"))
        .andExpect(jsonPath("$[1].conversationType").value("received"))
        .andExpect(jsonPath("$[1].swapType").value("ByBooks"))
        .andExpect(jsonPath("$[1].swapStatus").value("Pending"))
        .andExpect(jsonPath("$[1].unreadMessageCount").value(2))
        .andExpect(jsonPath("$[1].hasNewMessages").value(true));

    verify(inboxService).getUnifiedInbox("receiver123", null, null);
    verify(inboxService).getUnreadMessageCount("receiver123", "received1");
    verify(inboxService).getUnreadMessageCount("receiver123", "sent1");
    verify(inboxService).isInboxItemUnread(receivedSwap, "receiver123");
    verify(inboxService).isInboxItemUnread(sentSwap, "receiver123");
  }

  @Test
  @DisplayName("Should return unified inbox with status filter")
  void shouldReturnUnifiedInboxWithStatusFilter() throws Exception {
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
    swapRequest.setSwapType(SwapType.BY_BOOKS);
    swapRequest.setSwapStatus(SwapStatus.PENDING);
    swapRequest.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest.setAskForGiveaway(false);

    List<SwapRequest> swapRequests = Arrays.asList(swapRequest);
    when(inboxService.getUnifiedInbox("receiver123", "Pending", "latest_message")).thenReturn(swapRequests);
    when(inboxService.getUnreadMessageCount("receiver123", "swap1")).thenReturn(1L);
    when(inboxService.isInboxItemUnread(swapRequest, "receiver123")).thenReturn(true);

    // When & Then
    mockMvc.perform(get(API_PATH)
        .param("userId", "receiver123")
        .param("status", "Pending")
        .param("sortBy", "latest_message"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("swap1"))
        .andExpect(jsonPath("$[0].swapStatus").value("Pending"))
        .andExpect(jsonPath("$[0].unreadMessageCount").value(1))
        .andExpect(jsonPath("$[0].conversationType").value("received"));

    verify(inboxService).getUnifiedInbox("receiver123", "Pending", "latest_message");
    verify(inboxService).getUnreadMessageCount("receiver123", "swap1");
    verify(inboxService).isInboxItemUnread(swapRequest, "receiver123");
  }

  @Test
  @DisplayName("Should return 400 when invalid status provided")
  void shouldReturn400WhenInvalidStatusProvided() throws Exception {
    // Given
    when(inboxService.getUnifiedInbox("receiver123", "InvalidStatus", null))
        .thenThrow(new BadRequestException("invalidSwapStatus", "InvalidStatus"));

    // When & Then
    mockMvc.perform(get(API_PATH)
        .param("userId", "receiver123")
        .param("status", "InvalidStatus"))
        .andExpect(status().isBadRequest());

    verify(inboxService).getUnifiedInbox("receiver123", "InvalidStatus", null);
    verifyNoMoreInteractions(inboxService);
  }

  @Test
  @DisplayName("Should return 404 when user not found")
  void shouldReturn404WhenUserNotFound() throws Exception {
    // Given
    when(inboxService.getUnifiedInbox("nonexistent", null, null))
        .thenThrow(new UserNotFoundException());

    // When & Then
    mockMvc.perform(get(API_PATH)
        .param("userId", "nonexistent"))
        .andExpect(status().isNotFound());

    verify(inboxService).getUnifiedInbox("nonexistent", null, null);
    verifyNoMoreInteractions(inboxService);
  }

  @Test
  @DisplayName("Should return unified inbox with sorting by book title")
  void shouldReturnUnifiedInboxWithSorting() throws Exception {
    // Given
    User sender = new User();
    sender.setId("sender123");
    sender.setFirstName("John");
    sender.setLastName("Doe");

    User receiver = new User();
    receiver.setId("receiver123");
    receiver.setFirstName("Jane");
    receiver.setLastName("Smith");

    Book book1 = new Book();
    book1.setId("book1");
    book1.setTitle("A Test Book");
    book1.setAuthor("Test Author");

    Book book2 = new Book();
    book2.setId("book2");
    book2.setTitle("Z Test Book");
    book2.setAuthor("Test Author");

    SwapRequest swapRequest1 = new SwapRequest();
    swapRequest1.setId("swap1");
    swapRequest1.setSender(sender);
    swapRequest1.setReceiver(receiver);
    swapRequest1.setBookToSwapWith(book1);
    swapRequest1.setSwapType(SwapType.BY_BOOKS);
    swapRequest1.setSwapStatus(SwapStatus.PENDING);
    swapRequest1.setRequestedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest1.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));
    swapRequest1.setAskForGiveaway(false);

    SwapRequest swapRequest2 = new SwapRequest();
    swapRequest2.setId("swap2");
    swapRequest2.setSender(sender);
    swapRequest2.setReceiver(receiver);
    swapRequest2.setBookToSwapWith(book2);
    swapRequest2.setSwapType(SwapType.BY_BOOKS);
    swapRequest2.setSwapStatus(SwapStatus.PENDING);
    swapRequest2.setRequestedAt(Instant.parse("2025-01-02T10:00:00Z"));
    swapRequest2.setUpdatedAt(Instant.parse("2025-01-02T10:00:00Z"));
    swapRequest2.setAskForGiveaway(false);

    List<SwapRequest> swapRequests = Arrays.asList(swapRequest1, swapRequest2); // Sorted alphabetically by book title
    when(inboxService.getUnifiedInbox("receiver123", null, "book_title")).thenReturn(swapRequests);
    when(inboxService.getUnreadMessageCount("receiver123", "swap1")).thenReturn(0L);
    when(inboxService.getUnreadMessageCount("receiver123", "swap2")).thenReturn(0L);
    when(inboxService.isInboxItemUnread(swapRequest1, "receiver123")).thenReturn(false);
    when(inboxService.isInboxItemUnread(swapRequest2, "receiver123")).thenReturn(false);

    // When & Then
    mockMvc.perform(get(API_PATH)
        .param("userId", "receiver123")
        .param("sortBy", "book_title"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("swap1"))
        .andExpect(jsonPath("$[0].bookToSwapWith.title").value("A Test Book"))
        .andExpect(jsonPath("$[1].id").value("swap2"))
        .andExpect(jsonPath("$[1].bookToSwapWith.title").value("Z Test Book"));

    verify(inboxService).getUnifiedInbox("receiver123", null, "book_title");
    verify(inboxService).getUnreadMessageCount("receiver123", "swap1");
    verify(inboxService).getUnreadMessageCount("receiver123", "swap2");
    verify(inboxService).isInboxItemUnread(swapRequest1, "receiver123");
    verify(inboxService).isInboxItemUnread(swapRequest2, "receiver123");
  }
}
