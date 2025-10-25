/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kirjaswappi.backend.http.dtos.requests.SendMessageRequest;
import com.kirjaswappi.backend.http.dtos.responses.ChatMessageResponse;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.entities.ChatMessage;
import com.kirjaswappi.backend.service.entities.SwapRequest;

@Controller
public class RealtimeChatController {
  private static final Logger logger = LoggerFactory.getLogger(RealtimeChatController.class);

  @Autowired
  private ChatService chatService;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat/{swapRequestId}/send")
  public void sendMessage(
      @DestinationVariable String swapRequestId,
      @Payload SendMessageRequest request,
      Principal principal) {

    try {
      String userId = principal.getName();
      logger.debug("Received real-time message for swap request: {} from user: {}", swapRequestId, userId);

      // Validate message content
      if (!request.isValid()) {
        logger.warn("Invalid message content from user: {}", userId);
        return;
      }

      // Send message through existing service
      ChatMessage message = chatService.sendMessage(swapRequestId, userId, request.getMessage());

      // Get swap request details for context
      SwapRequest swapRequest = chatService.getSwapRequestForChat(swapRequestId, userId);

      // Create response
      ChatMessageResponse response = new ChatMessageResponse(message, userId);
      response.setSwapContext(createSwapContext(swapRequest));

      // Determine receiver ID
      String receiverId = swapRequest.getSender().getId().equals(userId)
          ? swapRequest.getReceiver().getId()
          : swapRequest.getSender().getId();

      // Send to both sender and receiver via WebSocket
      messagingTemplate.convertAndSendToUser(userId, "/queue/chat/" + swapRequestId, response);
      messagingTemplate.convertAndSendToUser(receiverId, "/queue/chat/" + swapRequestId, response);

      logger.debug("Real-time message sent successfully for swap request: {}", swapRequestId);

    } catch (Exception e) {
      logger.error("Error sending real-time message for swap request: {}", swapRequestId, e);
    }
  }

  private ChatMessageResponse.SwapContextResponse createSwapContext(SwapRequest swapRequest) {
    ChatMessageResponse.SwapContextResponse context = new ChatMessageResponse.SwapContextResponse();

    // Set basic swap information
    context.setSwapType(swapRequest.getSwapType().getCode());
    context.setSwapStatus(swapRequest.getSwapStatus().getCode());
    context.setAskForGiveaway(swapRequest.isAskForGiveaway());

    // Set requested book information
    context.setRequestedBook(
        new ChatMessageResponse.SwapContextResponse.BookInfoResponse(swapRequest.getBookToSwapWith()));

    // Set offered book/genre information if available
    if (swapRequest.getSwapOffer() != null) {
      if (swapRequest.getSwapOffer().getOfferedBook() != null) {
        context.setOfferedBook(
            new ChatMessageResponse.SwapContextResponse.BookInfoResponse(swapRequest.getSwapOffer().getOfferedBook()));
      }
      if (swapRequest.getSwapOffer().getOfferedGenre() != null) {
        context.setOfferedGenreName(swapRequest.getSwapOffer().getOfferedGenre().getName());
      }
    }

    return context;
  }
}
