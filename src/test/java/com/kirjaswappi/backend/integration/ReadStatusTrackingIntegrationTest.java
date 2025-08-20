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
import org.springframework.test.context.ActiveProfiles;

import com.kirjaswappi.backend.http.dtos.responses.ChatMessageResponse;
import com.kirjaswappi.backend.http.dtos.responses.InboxItemResponse;
import com.kirjaswappi.backend.jpa.daos.*;
import com.kirjaswappi.backend.jpa.repositories.*;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.*;

@SpringBootTest
@ActiveProfiles("test")
public class ReadStatusTrackingIntegrationTest {
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
    senderUser = new UserDao();
    senderUser.setFirstName("John");
    senderUser.setLastName("Sender");
    senderUser.setEmail("john.sender@test.com");
    senderUser.setPassword("password");
    senderUser.setEmailVerified(true);
    senderUser.setSalt("salt");
    senderUser = userRepository.save(senderUser);

    receiverUser = new UserDao();
    receiverUser.setFirstName("Jane");
    receiverUser.setLastName("Receiver");
    receiverUser.setEmail("jane.receiver@test.com");
    receiverUser.setPassword("password");
    receiverUser.setEmailVerified(true);
    receiverUser.setSalt("salt");
    receiverUser = userRepository.save(receiverUser);

    // Create test book
    testBook = new BookDao();
    testBook.setTitle("Test Book");
    testBook.setAuthor("Test Author");
    testBook.setCondition("Good");
    testBook.setLanguage("English");
    testBook.setOwner(receiverUser);
    testBook.setCoverPhotos(List.of());
    testBook.setGenres(List.of());
    var swapConditionDao = new SwapConditionDao();
    swapConditionDao.setSwapType("GiveAway");
    swapConditionDao.setGiveAway(true);
    testBook.setSwapCondition(swapConditionDao);
    testBook = bookRepository.save(testBook);

    // Create test swap request
    testSwapRequest = new SwapRequestDao();
    testSwapRequest.setSender(senderUser);
    testSwapRequest.setReceiver(receiverUser);
    testSwapRequest.setBookToSwapWith(testBook);
    testSwapRequest.setSwapType("ByBooks");
    testSwapRequest.setAskForGiveaway(false);
    testSwapRequest.setSwapStatus("Pending");
    testSwapRequest.setNote("Test swap request");
    testSwapRequest.setRequestedAt(Instant.now());
    testSwapRequest.setUpdatedAt(Instant.now());
    testSwapRequest = swapRequestRepository.save(testSwapRequest);
  }

  @Test
  void testInboxItemReadStatusTracking() {
    // Initially, the swap request should be unread for the receiver
    List<SwapRequest> receivedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), null, null);
    assertEquals(1, receivedRequests.size());

    SwapRequest swapRequest = receivedRequests.getFirst();
    assertTrue(inboxService.isInboxItemUnread(swapRequest, receiverUser.getId()));
    assertTrue(inboxService.isInboxItemUnread(swapRequest, senderUser.getId())); // Sender hasn't viewed it yet

    // Mark as read by receiver
    inboxService.markInboxItemAsRead(testSwapRequest.getId(), receiverUser.getId());

    // Verify it's now marked as read for receiver
    SwapRequestDao updatedRequest = swapRequestRepository.findById(testSwapRequest.getId()).orElseThrow();
    assertNotNull(updatedRequest.getReadByReceiverAt());
    assertNull(updatedRequest.getReadBySenderAt());

    // Verify the service method reflects this
    SwapRequest updatedEntity = inboxService.getUnifiedInbox(receiverUser.getId(), null, null).getFirst();
    assertFalse(inboxService.isInboxItemUnread(updatedEntity, receiverUser.getId()));

    // Mark as read by sender
    inboxService.markInboxItemAsRead(testSwapRequest.getId(), senderUser.getId());

    // Verify both users have read it
    updatedRequest = swapRequestRepository.findById(testSwapRequest.getId()).orElseThrow();
    assertNotNull(updatedRequest.getReadByReceiverAt());
    assertNotNull(updatedRequest.getReadBySenderAt());
  }

  @Test
  void testChatMessageReadStatusTracking() {
    // Send a message from sender to receiver
    ChatMessage message1 = chatService.sendMessage(testSwapRequest.getId(), senderUser.getId(), "Hello from sender");
    assertFalse(message1.isReadByReceiver());

    // Check unread count for receiver
    long unreadCount = chatService.getUnreadMessageCount(testSwapRequest.getId(), receiverUser.getId());
    assertEquals(1, unreadCount);

    // Receiver accesses chat - should automatically mark messages as read
    List<ChatMessage> messages = chatService.getChatMessages(testSwapRequest.getId(), receiverUser.getId());
    assertEquals(1, messages.size());

    // Verify messages are now marked as read
    unreadCount = chatService.getUnreadMessageCount(testSwapRequest.getId(), receiverUser.getId());
    assertEquals(0, unreadCount);

    // Send another message from receiver to sender
    ChatMessage message2 = chatService.sendMessage(testSwapRequest.getId(), receiverUser.getId(),
        "Hello back from receiver");
    assertFalse(message2.isReadByReceiver());

    // Check unread count for sender
    unreadCount = chatService.getUnreadMessageCount(testSwapRequest.getId(), senderUser.getId());
    assertEquals(1, unreadCount);

    // Sender accesses chat
    messages = chatService.getChatMessages(testSwapRequest.getId(), senderUser.getId());
    assertEquals(2, messages.size());

    // Verify no unread messages for sender
    unreadCount = chatService.getUnreadMessageCount(testSwapRequest.getId(), senderUser.getId());
    assertEquals(0, unreadCount);
  }

  @Test
  void testInboxItemResponseNotificationIndicators() {
    // Send a chat message to create unread messages
    chatService.sendMessage(testSwapRequest.getId(), senderUser.getId(), "Test message");

    // Get inbox items for receiver
    List<SwapRequest> receivedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), null, null);
    SwapRequest swapRequest = receivedRequests.getFirst();

    // Create response DTO and set indicators
    InboxItemResponse response = new InboxItemResponse(swapRequest);
    long unreadCount = inboxService.getUnreadMessageCount(receiverUser.getId(), swapRequest.getId());
    response.setUnreadMessageCount(unreadCount);
    response.setUnread(inboxService.isInboxItemUnread(swapRequest, receiverUser.getId()));
    response.setHasNewMessages(unreadCount > 0);

    // Verify notification indicators
    assertTrue(response.isUnread()); // Inbox item is unread
    assertTrue(response.isHasNewMessages()); // Has unread messages
    assertEquals(1, response.getUnreadMessageCount());

    // Mark inbox item as read
    inboxService.markInboxItemAsRead(testSwapRequest.getId(), receiverUser.getId());

    // Update response
    SwapRequest updatedSwapRequest = inboxService.getUnifiedInbox(receiverUser.getId(), null, null).getFirst();
    response = new InboxItemResponse(updatedSwapRequest);
    unreadCount = inboxService.getUnreadMessageCount(receiverUser.getId(), updatedSwapRequest.getId());
    response.setUnreadMessageCount(unreadCount);
    response.setUnread(inboxService.isInboxItemUnread(updatedSwapRequest, receiverUser.getId()));
    response.setHasNewMessages(unreadCount > 0);

    // Verify inbox item is now read but still has unread messages
    assertFalse(response.isUnread()); // Inbox item is read
    assertTrue(response.isHasNewMessages()); // Still has unread messages
    assertEquals(1, response.getUnreadMessageCount());
  }

  @Test
  void testChatMessageResponseNotificationIndicators() {
    // Send messages from both users
    ChatMessage message1 = chatService.sendMessage(testSwapRequest.getId(), senderUser.getId(), "Message from sender");
    ChatMessage message2 = chatService.sendMessage(testSwapRequest.getId(), receiverUser.getId(),
        "Message from receiver");

    // Get messages as receiver
    List<ChatMessage> messages = chatService.getChatMessages(testSwapRequest.getId(), receiverUser.getId());
    assertEquals(2, messages.size());

    // Create response DTOs with current user context
    List<ChatMessageResponse> responses = messages.stream()
        .map(message -> new ChatMessageResponse(message, receiverUser.getId()))
        .toList();

    // Verify isOwnMessage indicators
    assertFalse(responses.get(0).isOwnMessage()); // First message is from sender
    assertTrue(responses.get(1).isOwnMessage()); // Second message is from receiver

    // Get messages as sender
    messages = chatService.getChatMessages(testSwapRequest.getId(), senderUser.getId());
    responses = messages.stream()
        .map(message -> new ChatMessageResponse(message, senderUser.getId()))
        .toList();

    // Verify isOwnMessage indicators from sender's perspective
    assertTrue(responses.get(0).isOwnMessage()); // First message is from sender
    assertFalse(responses.get(1).isOwnMessage()); // Second message is from receiver
  }

  @Test
  void testReadStatusWorkflowIntegration() {
    // 1. Sender creates swap request - receiver should see it as unread
    List<SwapRequest> receivedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), null, null);
    SwapRequest swapRequest = receivedRequests.getFirst();
    assertTrue(inboxService.isInboxItemUnread(swapRequest, receiverUser.getId()));

    // 2. Sender sends a message
    chatService.sendMessage(testSwapRequest.getId(), senderUser.getId(), "Hi, interested in your book!");
    assertEquals(1, chatService.getUnreadMessageCount(testSwapRequest.getId(), receiverUser.getId()));

    // 3. Receiver views inbox - item should be marked as read but still have unread
    // messages
    receivedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), null, null);
    swapRequest = receivedRequests.getFirst();

    InboxItemResponse inboxResponse = new InboxItemResponse(swapRequest);
    long unreadCount = inboxService.getUnreadMessageCount(receiverUser.getId(), swapRequest.getId());
    inboxResponse.setUnreadMessageCount(unreadCount);
    inboxResponse.setUnread(inboxService.isInboxItemUnread(swapRequest, receiverUser.getId()));
    inboxResponse.setHasNewMessages(unreadCount > 0);

    // After viewing inbox, item should be marked as read
    inboxService.markInboxItemAsRead(testSwapRequest.getId(), receiverUser.getId());

    // 4. Receiver opens chat - messages should be marked as read
    List<ChatMessage> messages = chatService.getChatMessages(testSwapRequest.getId(), receiverUser.getId());
    assertEquals(1, messages.size());
    assertEquals(0, chatService.getUnreadMessageCount(testSwapRequest.getId(), receiverUser.getId()));

    // 5. Receiver responds
    chatService.sendMessage(testSwapRequest.getId(), receiverUser.getId(), "Yes, let's discuss!");

    // 6. Sender should now have unread messages
    assertEquals(1, chatService.getUnreadMessageCount(testSwapRequest.getId(), senderUser.getId()));

    // 7. Sender views sent requests - should see unread message indicator
    List<SwapRequest> sentRequests = inboxService.getUnifiedInbox(senderUser.getId(), null, null);
    SwapRequest sentRequest = sentRequests.getFirst();

    InboxItemResponse sentResponse = new InboxItemResponse(sentRequest);
    unreadCount = inboxService.getUnreadMessageCount(senderUser.getId(), sentRequest.getId());
    sentResponse.setUnreadMessageCount(unreadCount);
    sentResponse.setHasNewMessages(unreadCount > 0);

    assertTrue(sentResponse.isHasNewMessages());
    assertEquals(1, sentResponse.getUnreadMessageCount());
  }

  @Test
  void testMultipleUsersReadStatusIsolation() {
    // Create another user
    UserDao anotherUser = new UserDao();
    anotherUser.setFirstName("Bob");
    anotherUser.setLastName("Another");
    anotherUser.setEmail("bob.another@test.com");
    anotherUser.setPassword("password");
    anotherUser.setEmailVerified(true);
    anotherUser.setSalt("salt");
    final UserDao finalAnotherUser = userRepository.save(anotherUser);

    // Send messages from both sender and receiver
    chatService.sendMessage(testSwapRequest.getId(), senderUser.getId(), "Message 1");
    chatService.sendMessage(testSwapRequest.getId(), receiverUser.getId(), "Message 2");

    // Receiver reads messages
    chatService.getChatMessages(testSwapRequest.getId(), receiverUser.getId());
    assertEquals(0, chatService.getUnreadMessageCount(testSwapRequest.getId(), receiverUser.getId()));
    assertEquals(1, chatService.getUnreadMessageCount(testSwapRequest.getId(), senderUser.getId()));

    // Another user should not be able to access this chat
    assertThrows(Exception.class, () -> {
      chatService.getChatMessages(testSwapRequest.getId(), finalAnotherUser.getId());
    });

    // Mark inbox item as read for receiver
    inboxService.markInboxItemAsRead(testSwapRequest.getId(), receiverUser.getId());

    // Verify read status is isolated per user
    SwapRequestDao updatedRequest = swapRequestRepository.findById(testSwapRequest.getId()).orElseThrow();
    assertNotNull(updatedRequest.getReadByReceiverAt());
    assertNull(updatedRequest.getReadBySenderAt());

    // Mark as read for sender
    inboxService.markInboxItemAsRead(testSwapRequest.getId(), senderUser.getId());
    updatedRequest = swapRequestRepository.findById(testSwapRequest.getId()).orElseThrow();
    assertNotNull(updatedRequest.getReadByReceiverAt());
    assertNotNull(updatedRequest.getReadBySenderAt());
  }
}