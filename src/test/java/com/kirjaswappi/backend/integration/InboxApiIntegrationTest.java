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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.InboxController;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.*;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

/**
 * Integration tests for Inbox API endpoints. Tests unified inbox retrieval,
 * filtering, and sorting.
 */
@WebMvcTest(InboxController.class)
@Import(CustomMockMvcConfiguration.class)
class InboxApiIntegrationTest {

  private static final String API_BASE = "/api/v1/inbox";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private InboxService inboxService;

  private SwapRequest createTestSwapRequest(String id, String senderId, String receiverId, SwapStatus status) {
    User sender = User.builder()
        .id(senderId)
        .firstName("Sender")
        .lastName("User")
        .email("sender@test.com")
        .build();

    User receiver = User.builder()
        .id(receiverId)
        .firstName("Receiver")
        .lastName("User")
        .email("receiver@test.com")
        .build();

    Book book = Book.builder()
        .id("book-1")
        .title("Test Book")
        .author("Author")
        .language(Language.ENGLISH)
        .condition(Condition.GOOD)
        .owner(receiver)
        .coverPhotos(List.of())
        .genres(List.of())
        .build();

    return SwapRequest.builder()
        .id(id)
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.GIVE_AWAY)
        .swapStatus(status)
        .askForGiveaway(false)
        .note("Test note")
        .requestedAt(Instant.now())
        .build();
  }

  @Nested
  @DisplayName("Get Unified Inbox Tests")
  class GetUnifiedInboxTests {

    @Test
    @DisplayName("Should return unified inbox successfully")
    void shouldReturnUnifiedInboxSuccessfully() throws Exception {
      String userId = "user-1";
      List<SwapRequest> requests = List.of(
          createTestSwapRequest("request-1", userId, "user-2", SwapStatus.PENDING),
          createTestSwapRequest("request-2", "user-3", userId, SwapStatus.ACCEPTED));

      when(inboxService.getUnifiedInbox(userId, null, null)).thenReturn(requests);
      when(inboxService.getUnreadMessageCount(eq(userId), anyString())).thenReturn(0L);
      when(inboxService.isInboxItemUnread(any(), eq(userId))).thenReturn(false);

      mockMvc.perform(get(API_BASE)
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value("request-1"))
          .andExpect(jsonPath("$[1].id").value("request-2"));
    }

    @Test
    @DisplayName("Should return empty inbox when no swap requests")
    void shouldReturnEmptyInboxWhenNoSwapRequests() throws Exception {
      String userId = "user-1";
      when(inboxService.getUnifiedInbox(userId, null, null)).thenReturn(List.of());

      mockMvc.perform(get(API_BASE)
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should filter inbox by status")
    void shouldFilterInboxByStatus() throws Exception {
      String userId = "user-1";
      List<SwapRequest> requests = List.of(
          createTestSwapRequest("request-1", userId, "user-2", SwapStatus.PENDING));

      when(inboxService.getUnifiedInbox(userId, "Pending", null)).thenReturn(requests);
      when(inboxService.getUnreadMessageCount(eq(userId), anyString())).thenReturn(0L);
      when(inboxService.isInboxItemUnread(any(), eq(userId))).thenReturn(false);

      mockMvc.perform(get(API_BASE)
          .param("userId", userId)
          .param("status", "Pending"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].swapStatus").value("Pending"));
    }

    @Test
    @DisplayName("Should sort inbox by specified field")
    void shouldSortInboxBySpecifiedField() throws Exception {
      String userId = "user-1";
      List<SwapRequest> requests = List.of(
          createTestSwapRequest("request-1", userId, "user-2", SwapStatus.PENDING));

      when(inboxService.getUnifiedInbox(userId, null, "date")).thenReturn(requests);
      when(inboxService.getUnreadMessageCount(eq(userId), anyString())).thenReturn(0L);
      when(inboxService.isInboxItemUnread(any(), eq(userId))).thenReturn(false);

      mockMvc.perform(get(API_BASE)
          .param("userId", userId)
          .param("sortBy", "date"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1));

      verify(inboxService).getUnifiedInbox(userId, null, "date");
    }

    @Test
    @DisplayName("Should return 400 when userId is missing")
    void shouldReturn400WhenUserIdMissing() throws Exception {
      mockMvc.perform(get(API_BASE))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for invalid status")
    void shouldReturn400ForInvalidStatus() throws Exception {
      String userId = "user-1";
      when(inboxService.getUnifiedInbox(userId, "InvalidStatus", null))
          .thenThrow(new BadRequestException("invalidStatus", "InvalidStatus"));

      mockMvc.perform(get(API_BASE)
          .param("userId", userId)
          .param("status", "InvalidStatus"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should include unread message count in response")
    void shouldIncludeUnreadMessageCount() throws Exception {
      String userId = "user-1";
      List<SwapRequest> requests = List.of(
          createTestSwapRequest("request-1", userId, "user-2", SwapStatus.PENDING));

      when(inboxService.getUnifiedInbox(userId, null, null)).thenReturn(requests);
      when(inboxService.getUnreadMessageCount(userId, "request-1")).thenReturn(5L);
      when(inboxService.isInboxItemUnread(any(), eq(userId))).thenReturn(true);

      mockMvc.perform(get(API_BASE)
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].unreadMessageCount").value(5))
          .andExpect(jsonPath("$[0].unread").value(true))
          .andExpect(jsonPath("$[0].hasNewMessages").value(true));
    }

    @Test
    @DisplayName("Should set conversation type correctly for sent requests")
    void shouldSetConversationTypeForSentRequests() throws Exception {
      String userId = "user-1";
      List<SwapRequest> requests = List.of(
          createTestSwapRequest("request-1", userId, "user-2", SwapStatus.PENDING));

      when(inboxService.getUnifiedInbox(userId, null, null)).thenReturn(requests);
      when(inboxService.getUnreadMessageCount(eq(userId), anyString())).thenReturn(0L);
      when(inboxService.isInboxItemUnread(any(), eq(userId))).thenReturn(false);

      mockMvc.perform(get(API_BASE)
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].conversationType").value("sent"));
    }

    @Test
    @DisplayName("Should set conversation type correctly for received requests")
    void shouldSetConversationTypeForReceivedRequests() throws Exception {
      String userId = "user-2";
      List<SwapRequest> requests = List.of(
          createTestSwapRequest("request-1", "user-1", userId, SwapStatus.PENDING));

      when(inboxService.getUnifiedInbox(userId, null, null)).thenReturn(requests);
      when(inboxService.getUnreadMessageCount(eq(userId), anyString())).thenReturn(0L);
      when(inboxService.isInboxItemUnread(any(), eq(userId))).thenReturn(false);

      mockMvc.perform(get(API_BASE)
          .param("userId", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].conversationType").value("received"));
    }
  }
}
