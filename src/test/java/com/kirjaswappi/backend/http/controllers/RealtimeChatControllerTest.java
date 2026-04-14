/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.kirjaswappi.backend.http.dtos.requests.SendMessageRequest;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.*;

class RealtimeChatControllerTest {

  @Mock
  private ChatService chatService;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private RealtimeChatController realtimeChatController;

  private User sender;
  private User receiver;
  private SwapRequest swapRequest;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    sender = new User().id("user-1").firstName("Alice").lastName("Smith");
    receiver = new User().id("user-2").firstName("Bob").lastName("Jones");

    Book book = Book.builder()
        .id("book-1")
        .title("Test Book")
        .author("Author")
        .condition(Condition.GOOD)
        .coverPhotos(List.of())
        .build();

    swapRequest = SwapRequest.builder()
        .id("swap-1")
        .sender(sender)
        .receiver(receiver)
        .bookToSwapWith(book)
        .swapType(SwapType.GIVE_AWAY)
        .swapStatus(SwapStatus.PENDING)
        .askForGiveaway(false)
        .build();
  }

  @Test
  @DisplayName("Should send message to both sender and receiver")
  void shouldSendMessageToBothUsers() {
    String swapRequestId = "swap-1";
    String userId = "user-1";

    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("Hello!");

    ChatMessage message = ChatMessage.builder()
        .id("msg-1")
        .swapRequestId(swapRequestId)
        .sender(sender)
        .message("Hello!")
        .sentAt(Instant.now())
        .readByReceiver(false)
        .build();

    when(chatService.sendMessage(swapRequestId, userId, "Hello!")).thenReturn(message);
    when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

    realtimeChatController.sendMessage(swapRequestId, request, () -> userId);

    // Verify message sent to sender
    verify(messagingTemplate).convertAndSendToUser(
        eq("user-1"), eq("/queue/chat.swap-1"), any());
    // Verify message sent to receiver
    verify(messagingTemplate).convertAndSendToUser(
        eq("user-2"), eq("/queue/chat.swap-1"), any());
  }

  @Test
  @DisplayName("Should send message when receiver is the sender of the STOMP message")
  void shouldSendMessageWhenReceiverSendsMessage() {
    String swapRequestId = "swap-1";
    String userId = "user-2"; // Receiver of the swap is sending a chat message

    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("Sure!");

    ChatMessage message = ChatMessage.builder()
        .id("msg-2")
        .swapRequestId(swapRequestId)
        .sender(receiver)
        .message("Sure!")
        .sentAt(Instant.now())
        .readByReceiver(false)
        .build();

    when(chatService.sendMessage(swapRequestId, userId, "Sure!")).thenReturn(message);
    when(chatService.getSwapRequestForChat(swapRequestId, userId)).thenReturn(swapRequest);

    realtimeChatController.sendMessage(swapRequestId, request, () -> userId);

    // Verify message sent to user-2 (the current sender)
    verify(messagingTemplate).convertAndSendToUser(
        eq("user-2"), eq("/queue/chat.swap-1"), any());
    // Verify message sent to user-1 (the other party)
    verify(messagingTemplate).convertAndSendToUser(
        eq("user-1"), eq("/queue/chat.swap-1"), any());
  }

  @Test
  @DisplayName("Should not send message when request is invalid")
  void shouldNotSendMessageWhenRequestInvalid() {
    SendMessageRequest request = new SendMessageRequest();
    // Both message and images are null → isValid() returns false

    realtimeChatController.sendMessage("swap-1", request, () -> "user-1");

    verify(chatService, never()).sendMessage(any(), any(), any());
    verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(Object.class));
  }

  @Test
  @DisplayName("Should handle exception from chat service gracefully")
  void shouldHandleExceptionGracefully() {
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("Hello!");

    when(chatService.sendMessage(any(), any(), any())).thenThrow(new RuntimeException("DB error"));

    // Should not throw — exception is caught internally
    realtimeChatController.sendMessage("swap-1", request, () -> "user-1");

    verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(Object.class));
  }

  @Test
  @DisplayName("Should handle null principal gracefully")
  void shouldHandleNullPrincipalGracefully() {
    SendMessageRequest request = new SendMessageRequest();
    request.setMessage("Hello!");

    // Principal.getName() will throw NPE, caught by the try-catch
    realtimeChatController.sendMessage("swap-1", request, null);

    verify(chatService, never()).sendMessage(any(), any(), any());
    verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(Object.class));
  }
}
