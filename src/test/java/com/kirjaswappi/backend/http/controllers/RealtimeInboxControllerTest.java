/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.kirjaswappi.backend.events.InboxUpdateEvent;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;

class RealtimeInboxControllerTest {

  @Mock
  private InboxService inboxService;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private RealtimeInboxController realtimeInboxController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("Should send delta update on inbox event")
  void shouldSendDeltaUpdateOnInboxEvent() {
    // Given
    String userId = "user1";
    String swapRequestId = "swap1";
    InboxUpdateEvent event = new InboxUpdateEvent(userId, swapRequestId, InboxUpdateEvent.STATUS_CHANGE);

    // Mock InboxService to return a dummy SwapRequest
    SwapRequest mockRequest = SwapRequest.builder()
        .id(swapRequestId)
        .sender(new User().id(userId).firstName("Test").lastName("User"))
        .receiver(new User().id("other").firstName("Other").lastName("User"))
        .bookToSwapWith(Book.builder().id("book1").title("Book").build())
        .swapType(SwapType.GIVE_AWAY)
        .swapStatus(SwapStatus.PENDING)
        .build();

    when(inboxService.getInboxItem(userId, swapRequestId)).thenReturn(mockRequest);
    when(inboxService.getUnreadMessageCount(userId, swapRequestId)).thenReturn(0L);

    // When
    realtimeInboxController.handleInboxUpdateEvent(event);

    // Then
    // Verify it calls getInboxItem (single fetch) NOT getUnifiedInbox (list fetch)
    verify(inboxService).getInboxItem(userId, swapRequestId);
    verify(inboxService, never()).getUnifiedInbox(any(), any(), any());

    // Verify it sends to the ITEM update queue
    verify(messagingTemplate).convertAndSendToUser(
        eq(userId),
        eq("/queue/inbox/item-update"), // Critical check: ensuring delta path
        any(Object.class));
  }
}
