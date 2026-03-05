/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import java.security.Principal;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;

import com.kirjaswappi.backend.events.InboxUpdateEvent;
import com.kirjaswappi.backend.http.dtos.responses.InboxItemResponse;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.SwapRequest;

@Controller
@RequiredArgsConstructor
@Tag(name = "Realtime Inbox", description = "WebSocket API for real-time inbox notifications")
public class RealtimeInboxController {
  private static final Logger logger = LoggerFactory.getLogger(RealtimeInboxController.class);

  private final InboxService inboxService;

  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/inbox/subscribe")
  public void subscribeToInbox(Principal principal) {
    try {
      String userId = principal.getName();
      logger.debug("User {} subscribed to real-time inbox updates", userId);

      // Send initial inbox data
      sendInboxUpdate(userId, null, null);

    } catch (Exception e) {
      logger.error("Error subscribing to inbox updates", e);
    }
  }

  @MessageMapping("/inbox/refresh")
  public void refreshInbox(Principal principal) {
    try {
      String userId = principal.getName();
      logger.debug("Refreshing inbox for user: {}", userId);

      // Send updated inbox data
      sendInboxUpdate(userId, null, null);

    } catch (Exception e) {
      logger.error("Error refreshing inbox", e);
    }
  }

  /**
   * Event listener that handles inbox update events. This method is called
   * asynchronously when InboxUpdateEvent is published.
   */
  @EventListener
  @Async("inboxEventExecutor")
  public void handleInboxUpdateEvent(InboxUpdateEvent event) {
    try {
      logger.debug("Handling inbox update event for user: {} (type: {})", event.getUserId(), event.getEventType());
      broadcastInboxUpdate(event.getUserId(), event.getSwapRequestId());
    } catch (Exception e) {
      logger.error("Error handling inbox update event for user: {}", event.getUserId(), e);
    }
  }

  /**
   * Broadcasts inbox update to a specific user. Made private as it should only be
   * called through event handling.
   */
  private void broadcastInboxUpdate(String userId, String swapRequestId) {
    try {
      sendInboxItemUpdate(userId, swapRequestId);
    } catch (Exception e) {
      logger.error("Error broadcasting inbox update to user: {}", userId, e);
    }
  }

  private void sendInboxUpdate(String userId, String status, String sortBy) {
    try {
      List<SwapRequest> swapRequests = inboxService.getUnifiedInbox(userId, status, sortBy);
      List<InboxItemResponse> response = swapRequests.stream()
          .map(swapRequest -> createInboxItemResponse(swapRequest, userId))
          .toList();

      messagingTemplate.convertAndSendToUser(userId, "/queue/inbox/update", response);
      logger.debug("Sent inbox update to user: {} with {} items", userId, response.size());

    } catch (Exception e) {
      logger.error("Error sending inbox update to user: {}", userId, e);
    }
  }

  private void sendInboxItemUpdate(String userId, String swapRequestId) {
    try {
      SwapRequest swapRequest = inboxService.getInboxItem(userId, swapRequestId);
      InboxItemResponse response = createInboxItemResponse(swapRequest, userId);

      messagingTemplate.convertAndSendToUser(userId, "/queue/inbox/item-update", response);
      logger.debug("Sent inbox item update to user: {} for swap request: {}", userId, swapRequestId);

    } catch (Exception e) {
      logger.error("Error sending inbox item update to user: {} for swap request: {}", userId, swapRequestId, e);
    }
  }

  private InboxItemResponse createInboxItemResponse(SwapRequest swapRequest, String userId) {
    InboxItemResponse item = new InboxItemResponse(swapRequest);
    // Add unread message count using cached version
    long unreadCount = inboxService.getUnreadMessageCount(userId, swapRequest.id());
    item.setUnreadMessageCount(unreadCount);
    // Set notification indicators
    item.setUnread(inboxService.isInboxItemUnread(swapRequest, userId));
    item.setHasNewMessages(unreadCount > 0);
    // Set conversation type for UI display
    item.setConversationType(userId.equals(swapRequest.sender().id()) ? "sent" : "received");
    return item;
  }
}
