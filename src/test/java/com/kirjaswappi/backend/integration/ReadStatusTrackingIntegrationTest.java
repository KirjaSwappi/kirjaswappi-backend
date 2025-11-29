/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.kirjaswappi.backend.config.TestContainersConfig;
import com.kirjaswappi.backend.http.dtos.responses.ChatMessageResponse;
import com.kirjaswappi.backend.http.dtos.responses.InboxItemResponse;
import com.kirjaswappi.backend.jpa.daos.*;
import com.kirjaswappi.backend.jpa.repositories.*;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.*;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ReadStatusTrackingIntegrationTest {
  @Autowired
  private InboxService inboxService;

  @Autowired
  private ChatService chatService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private SwapRequestRepository swapRequestRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  private UserDao senderUser;
  private UserDao receiverUser;
  private BookDao testBook;
  private SwapRequestDao testSwapRequest;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    chatMessageRepository.deleteAll();
    swapRequestRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();

    // Create test users
    senderUser = UserDao.builder()
        .firstName("John")
        .lastName("Sender")
        .email("john.sender@test.com")
        .password("password")
        .isEmailVerified(true)
        .salt("salt")
        .build();
    senderUser = userRepository.save(senderUser);

    receiverUser = UserDao.builder()
        .firstName("Jane")
        .lastName("Receiver")
        .email("jane.receiver@test.com")
        .password("password")
        .isEmailVerified(true)
        .salt("salt")
        .build();

    receiverUser = userRepository.save(receiverUser);

    var swapConditionDao = SwapConditionDao.builder()
        .swapType("GiveAway")
        .giveAway(true)
        .build();

    // Create test book
    testBook = BookDao.builder()
        .title("Test Book")
        .author("Test Author")
        .condition("Good")
        .language("English")
        .owner(receiverUser)
        .coverPhotos(List.of())
        .genres(List.of())
        .swapCondition(swapConditionDao)
        .build();

    testBook = bookRepository.save(testBook);

    // Create test swap request
    testSwapRequest = SwapRequestDao.builder()
        .sender(senderUser)
        .receiver(receiverUser)
        .bookToSwapWith(testBook)
        .swapType("ByBooks")
        .askForGiveaway(false)
        .swapStatus("Pending")
        .note("Test swap request")
        .requestedAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    testSwapRequest = swapRequestRepository.save(testSwapRequest);
  }

  @Test
  void testInboxItemReadStatusTracking() {
    // Initially, the swap request should be unread for the receiver
    List<SwapRequest> receivedRequests = inboxService.getUnifiedInbox(receiverUser.id(), null, null);
    assertEquals(1, receivedRequests.size());

    SwapRequest swapRequest = receivedRequests.getFirst();
    assertTrue(inboxService.isInboxItemUnread(swapRequest, receiverUser.id()));
    assertTrue(inboxService.isInboxItemUnread(swapRequest, senderUser.id())); // Sender hasn't viewed it yet

    // Mark as read by receiver
    inboxService.markInboxItemAsRead(testSwapRequest.id(), receiverUser.id());

    // Verify it's now marked as read for receiver
    SwapRequestDao updatedRequest = swapRequestRepository.findById(testSwapRequest.id()).orElseThrow();
    assertNotNull(updatedRequest.readByReceiverAt());
    assertNull(updatedRequest.readBySenderAt());

    // Verify the service method reflects this
    SwapRequest updatedEntity = inboxService.getUnifiedInbox(receiverUser.id(), null, null).getFirst();
    assertFalse(inboxService.isInboxItemUnread(updatedEntity, receiverUser.id()));

    // Mark as read by sender
    inboxService.markInboxItemAsRead(testSwapRequest.id(), senderUser.id());

    // Verify both users have read it
    updatedRequest = swapRequestRepository.findById(testSwapRequest.id()).orElseThrow();
    assertNotNull(updatedRequest.readByReceiverAt());
    assertNotNull(updatedRequest.readBySenderAt());
  }

  @Test
  void testChatMessageReadStatusTracking() {
    // Send a message from sender to receiver
    ChatMessage message1 = chatService.sendMessage(testSwapRequest.id(), senderUser.id(), "Hello from sender");
    assertFalse(message1.readByReceiver());

    // Check unread count for receiver
    long unreadCount = chatService.getUnreadMessageCount(testSwapRequest.id(), receiverUser.id());
    assertEquals(1, unreadCount);

    // Receiver accesses chat - should automatically mark messages as read
    List<ChatMessage> messages = chatService.getChatMessages(testSwapRequest.id(), receiverUser.id());
    assertEquals(1, messages.size());

    // Verify messages are now marked as read
    unreadCount = chatService.getUnreadMessageCount(testSwapRequest.id(), receiverUser.id());
    assertEquals(0, unreadCount);

    // Send another message from receiver to sender
    ChatMessage message2 = chatService.sendMessage(testSwapRequest.id(), receiverUser.id(),
        "Hello back from receiver");
    assertFalse(message2.readByReceiver());

    // Check unread count for sender
    unreadCount = chatService.getUnreadMessageCount(testSwapRequest.id(), senderUser.id());
    assertEquals(1, unreadCount);

    // Sender accesses chat
    messages = chatService.getChatMessages(testSwapRequest.id(), senderUser.id());
    assertEquals(2, messages.size());

    // Verify no unread messages for sender
    unreadCount = chatService.getUnreadMessageCount(testSwapRequest.id(), senderUser.id());
    assertEquals(0, unreadCount);
  }

  @Test
  void testInboxItemResponseNotificationIndicators() {
    // Send a chat message to create unread messages
    chatService.sendMessage(testSwapRequest.id(), senderUser.id(), "Test message");

    // Get inbox items for receiver
    List<SwapRequest> receivedRequests = inboxService.getUnifiedInbox(receiverUser.id(), null, null);
    SwapRequest swapRequest = receivedRequests.getFirst();

    // Create response DTO and set indicators
    InboxItemResponse response = new InboxItemResponse(swapRequest);
    long unreadCount = inboxService.getUnreadMessageCount(receiverUser.id(), swapRequest.id());
    response.setUnreadMessageCount(unreadCount);
    response.setUnread(inboxService.isInboxItemUnread(swapRequest, receiverUser.id()));
    response.setHasNewMessages(unreadCount > 0);

    // Verify notification indicators
    assertTrue(response.isUnread()); // Inbox item is unread
    assertTrue(response.isHasNewMessages()); // Has unread messages
    assertEquals(1, response.getUnreadMessageCount());

    // Mark inbox item as read
    inboxService.markInboxItemAsRead(testSwapRequest.id(), receiverUser.id());

    // Update response
    SwapRequest updatedSwapRequest = inboxService.getUnifiedInbox(receiverUser.id(), null, null).getFirst();
    response = new InboxItemResponse(updatedSwapRequest);
    unreadCount = inboxService.getUnreadMessageCount(receiverUser.id(), updatedSwapRequest.id());
    response.setUnreadMessageCount(unreadCount);
    response.setUnread(inboxService.isInboxItemUnread(updatedSwapRequest, receiverUser.id()));
    response.setHasNewMessages(unreadCount > 0);

    // Verify inbox item is now read but still has unread messages
    assertFalse(response.isUnread()); // Inbox item is read
    assertTrue(response.isHasNewMessages()); // Still has unread messages
    assertEquals(1, response.getUnreadMessageCount());
  }

  @Test
  void testChatMessageResponseNotificationIndicators() {
    // Create test messages
    chatService.sendMessage(testSwapRequest.id(), senderUser.id(), "Hello from sender");
    chatService.sendMessage(testSwapRequest.id(), receiverUser.id(), "Hello from receiver");

    // Get messages as receiver
    List<ChatMessage> messages = chatService.getChatMessages(testSwapRequest.id(), receiverUser.id());
    assertEquals(2, messages.size());

    // Create response DTOs with current user context
    List<ChatMessageResponse> responses = messages.stream()
        .map(message -> new ChatMessageResponse(message, receiverUser.id()))
        .toList();

    // Verify isOwnMessage indicators
    assertFalse(responses.get(0).isOwnMessage()); // First message is from sender
    assertTrue(responses.get(1).isOwnMessage()); // Second message is from receiver

    // Get messages as sender
    messages = chatService.getChatMessages(testSwapRequest.id(), senderUser.id());
    responses = messages.stream()
        .map(message -> new ChatMessageResponse(message, senderUser.id()))
        .toList();

    // Verify isOwnMessage indicators from sender's perspective
    assertTrue(responses.get(0).isOwnMessage()); // First message is from sender
    assertFalse(responses.get(1).isOwnMessage()); // Second message is from receiver
  }

  @Test
  void testReadStatusWorkflowIntegration() {
    // 1. Sender creates swap request - receiver should see it as unread
    List<SwapRequest> receivedRequests = inboxService.getUnifiedInbox(receiverUser.id(), null, null);
    SwapRequest swapRequest = receivedRequests.getFirst();
    assertTrue(inboxService.isInboxItemUnread(swapRequest, receiverUser.id()));

    // 2. Sender sends a message
    chatService.sendMessage(testSwapRequest.id(), senderUser.id(), "Hi, interested in your book!");
    assertEquals(1, chatService.getUnreadMessageCount(testSwapRequest.id(), receiverUser.id()));

    // 3. Receiver views inbox - item should be marked as read but still have unread
    // messages
    receivedRequests = inboxService.getUnifiedInbox(receiverUser.id(), null, null);
    swapRequest = receivedRequests.getFirst();

    InboxItemResponse inboxResponse = new InboxItemResponse(swapRequest);
    long unreadCount = inboxService.getUnreadMessageCount(receiverUser.id(), swapRequest.id());
    inboxResponse.setUnreadMessageCount(unreadCount);
    inboxResponse.setUnread(inboxService.isInboxItemUnread(swapRequest, receiverUser.id()));
    inboxResponse.setHasNewMessages(unreadCount > 0);

    // After viewing inbox, item should be marked as read
    inboxService.markInboxItemAsRead(testSwapRequest.id(), receiverUser.id());

    // 4. Receiver opens chat - messages should be marked as read
    List<ChatMessage> messages = chatService.getChatMessages(testSwapRequest.id(), receiverUser.id());
    assertEquals(1, messages.size());
    assertEquals(0, chatService.getUnreadMessageCount(testSwapRequest.id(), receiverUser.id()));

    // 5. Receiver responds
    chatService.sendMessage(testSwapRequest.id(), receiverUser.id(), "Yes, let's discuss!");

    // 6. Sender should now have unread messages
    assertEquals(1, chatService.getUnreadMessageCount(testSwapRequest.id(), senderUser.id()));

    // 7. Sender views sent requests - should see unread message indicator
    List<SwapRequest> sentRequests = inboxService.getUnifiedInbox(senderUser.id(), null, null);
    SwapRequest sentRequest = sentRequests.getFirst();

    InboxItemResponse sentResponse = new InboxItemResponse(sentRequest);
    unreadCount = inboxService.getUnreadMessageCount(senderUser.id(), sentRequest.id());
    sentResponse.setUnreadMessageCount(unreadCount);
    sentResponse.setHasNewMessages(unreadCount > 0);

    assertTrue(sentResponse.isHasNewMessages());
    assertEquals(1, sentResponse.getUnreadMessageCount());
  }

  @Test
  void testMultipleUsersReadStatusIsolation() {
    // Create another user
    var anotherUser = UserDao.builder()
        .firstName("Bob")
        .lastName("Another")
        .email("bob.another@test.com")
        .password("password")
        .isEmailVerified(true)
        .salt("salt")
        .build();
    final UserDao finalAnotherUser = userRepository.save(anotherUser);

    // Send messages from both sender and receiver
    chatService.sendMessage(testSwapRequest.id(), senderUser.id(), "Message 1");
    chatService.sendMessage(testSwapRequest.id(), receiverUser.id(), "Message 2");

    // Receiver reads messages
    chatService.getChatMessages(testSwapRequest.id(), receiverUser.id());
    assertEquals(0, chatService.getUnreadMessageCount(testSwapRequest.id(), receiverUser.id()));
    assertEquals(1, chatService.getUnreadMessageCount(testSwapRequest.id(), senderUser.id()));

    // Another user should not be able to access this chat
    assertThrows(Exception.class, () -> {
      chatService.getChatMessages(testSwapRequest.id(), finalAnotherUser.id());
    });

    // Mark inbox item as read for receiver
    inboxService.markInboxItemAsRead(testSwapRequest.id(), receiverUser.id());

    // Verify read status is isolated per user
    SwapRequestDao updatedRequest = swapRequestRepository.findById(testSwapRequest.id()).orElseThrow();
    assertNotNull(updatedRequest.readByReceiverAt());
    assertNull(updatedRequest.readBySenderAt());

    // Mark as read for sender
    inboxService.markInboxItemAsRead(testSwapRequest.id(), senderUser.id());
    updatedRequest = swapRequestRepository.findById(testSwapRequest.id()).orElseThrow();
    assertNotNull(updatedRequest.readByReceiverAt());
    assertNotNull(updatedRequest.readBySenderAt());
  }
}
