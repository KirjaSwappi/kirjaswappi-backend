/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class RealtimeInboxController {
  private static final Logger logger = LoggerFactory.getLogger(RealtimeInboxController.class);

  @Autowired
  private InboxService inboxService;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

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
      broadcastInboxUpdate(event.getUserId());
    } catch (Exception e) {
      logger.error("Error handling inbox update event for user: {}", event.getUserId(), e);
    }
  }

  /**
   * Broadcasts inbox update to a specific user. Made private as it should only be
   * called through event handling.
   */
  private void broadcastInboxUpdate(String userId) {
    try {
      sendInboxUpdate(userId, null, null);
    } catch (Exception e) {
      logger.error("Error broadcasting inbox update to user: {}", userId, e);
    }
  }

  private void sendInboxUpdate(String userId, String status, String sortBy) {
    try {
      List<SwapRequest> swapRequests = inboxService.getUnifiedInbox(userId, status, sortBy);
      List<InboxItemResponse> response = swapRequests.stream()
          .map(swapRequest -> {
            InboxItemResponse item = new InboxItemResponse(swapRequest);
            // Add unread message count using cached version
            long unreadCount = inboxService.getUnreadMessageCount(userId, swapRequest.getId());
            item.setUnreadMessageCount(unreadCount);
            // Set notification indicators
            item.setUnread(inboxService.isInboxItemUnread(swapRequest, userId));
            item.setHasNewMessages(unreadCount > 0);
            // Set conversation type for UI display
            item.setConversationType(userId.equals(swapRequest.getSender().getId()) ? "sent" : "received");
            return item;
          })
          .toList();

      messagingTemplate.convertAndSendToUser(userId, "/queue/inbox/update", response);
      logger.debug("Sent inbox update to user: {} with {} items", userId, response.size());

    } catch (Exception e) {
      logger.error("Error sending inbox update to user: {}", userId, e);
    }
  }
}
