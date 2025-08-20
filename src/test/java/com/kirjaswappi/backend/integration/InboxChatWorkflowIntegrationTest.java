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
import com.kirjaswappi.backend.service.SwapService;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.*;

/**
 * Comprehensive integration test for the complete inbox and chat workflow.
 * Tests end-to-end scenarios including inbox management, chat conversations,
 * status changes, filtering, sorting, and database consistency.
 */
@SpringBootTest
@ActiveProfiles("test")
public class InboxChatWorkflowIntegrationTest {
  @Autowired
  private InboxService inboxService;

  @Autowired
  private ChatService chatService;

  @Autowired
  private SwapService swapService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private SwapRequestRepository swapRequestRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Autowired
  private GenreRepository genreRepository;

  private UserDao senderUser;
  private UserDao receiverUser;
  private UserDao anotherUser;
  private BookDao testBook1;
  private BookDao testBook2;
  private SwapRequestDao testSwapRequest1;
  private SwapRequestDao testSwapRequest2;
  private SwapRequestDao testSwapRequest3;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    chatMessageRepository.deleteAll();
    swapRequestRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();
    genreRepository.deleteAll();

    // Create test users
    senderUser = createTestUser("John", "Sender", "john.sender@test.com");
    receiverUser = createTestUser("Jane", "Receiver", "jane.receiver@test.com");
    anotherUser = createTestUser("Bob", "Another", "bob.another@test.com");

    // Create test books
    testBook1 = createTestBook("Test Book 1", "Author 1", receiverUser);
    testBook2 = createTestBook("Another Book", "Author 2", receiverUser);

    // Create test swap requests with different statuses and timestamps
    testSwapRequest1 = createTestSwapRequest(senderUser, receiverUser, testBook1, "Pending",
        Instant.now().minusSeconds(3600), "Interested in your book!");
    testSwapRequest2 = createTestSwapRequest(anotherUser, receiverUser, testBook2, "Accepted",
        Instant.now().minusSeconds(1800), "Would love to swap!");
    testSwapRequest3 = createTestSwapRequest(receiverUser, senderUser, testBook1, "Rejected",
        Instant.now().minusSeconds(900), "Looking for this book");
  }

  @Test
  void testCompleteInboxWorkflow() {
    // 1. Receiver views unified inbox - should see both received requests (2) and
    // sent request (1) = 3 total
    List<SwapRequest> allRequests = inboxService.getUnifiedInbox(receiverUser.getId(), null, null);
    assertEquals(3, allRequests.size());

    // Filter to get only received requests (2)
    List<SwapRequest> receivedRequests = allRequests.stream()
        .filter(req -> req.getReceiver().getId().equals(receiverUser.getId()))
        .toList();
    assertEquals(2, receivedRequests.size());

    // Verify requests are ordered by most recent first (latest message or request
    // date)
    assertTrue(allRequests.get(0).getRequestedAt().isAfter(allRequests.get(2).getRequestedAt()));

    // 2. Create inbox item responses with notification indicators
    List<InboxItemResponse> inboxResponses = receivedRequests.stream()
        .map(request -> {
          InboxItemResponse response = new InboxItemResponse(request);
          long unreadCount = inboxService.getUnreadMessageCount(receiverUser.getId(), request.getId());
          response.setUnreadMessageCount(unreadCount);
          response.setUnread(inboxService.isInboxItemUnread(request, receiverUser.getId()));
          response.setHasNewMessages(unreadCount > 0);
          response.setConversationType("received");
          return response;
        })
        .toList();

    // Initially all items should be unread with no messages
    assertTrue(inboxResponses.get(0).isUnread());
    assertTrue(inboxResponses.get(1).isUnread());
    assertEquals(0, inboxResponses.get(0).getUnreadMessageCount());
    assertEquals(0, inboxResponses.get(1).getUnreadMessageCount());

    // 3. Sender sends a message to first swap request
    ChatMessage message1 = chatService.sendMessage(testSwapRequest1.getId(), senderUser.getId(),
        "Hi! I'm really interested in this book. Is it still available?");
    assertNotNull(message1);
    assertFalse(message1.isReadByReceiver());

    // 4. Receiver views inbox again - should see unread message indicator
    receivedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), null, null);
    SwapRequest requestWithMessage = receivedRequests.stream()
        .filter(r -> r.getId().equals(testSwapRequest1.getId()))
        .findFirst()
        .orElseThrow();

    long unreadCount = inboxService.getUnreadMessageCount(receiverUser.getId(), requestWithMessage.getId());
    assertEquals(1, unreadCount);

    // 5. Receiver marks inbox item as read
    inboxService.markInboxItemAsRead(testSwapRequest1.getId(), receiverUser.getId());

    // 6. Receiver opens chat - messages should be automatically marked as read
    List<ChatMessage> chatMessages = chatService.getChatMessages(testSwapRequest1.getId(), receiverUser.getId());
    assertEquals(1, chatMessages.size());
    assertEquals(0, chatService.getUnreadMessageCount(testSwapRequest1.getId(), receiverUser.getId()));

    // 7. Receiver responds in chat
    ChatMessage response = chatService.sendMessage(testSwapRequest1.getId(), receiverUser.getId(),
        "Yes, it's available! What would you like to offer?");
    assertNotNull(response);

    // 8. Receiver accepts the swap request
    SwapRequest updatedRequest = inboxService.updateSwapRequestStatus(testSwapRequest1.getId(),
        "Accepted", receiverUser.getId());
    assertEquals(SwapStatus.ACCEPTED, updatedRequest.getSwapStatus());

    // 9. Sender views sent requests - should see status change and unread message
    List<SwapRequest> sentRequests = inboxService.getUnifiedInbox(senderUser.getId(), null, null);
    SwapRequest sentRequest = sentRequests.stream()
        .filter(r -> r.getId().equals(testSwapRequest1.getId()))
        .findFirst()
        .orElseThrow();

    assertEquals(SwapStatus.ACCEPTED, sentRequest.getSwapStatus());
    assertEquals(1, chatService.getUnreadMessageCount(testSwapRequest1.getId(), senderUser.getId()));

    // 10. Verify database consistency
    SwapRequestDao dbRequest = swapRequestRepository.findById(testSwapRequest1.getId()).orElseThrow();
    assertEquals("Accepted", dbRequest.getSwapStatus());
    assertNotNull(dbRequest.getReadByReceiverAt());
    assertNull(dbRequest.getReadBySenderAt());

    List<ChatMessageDao> dbMessages = chatMessageRepository
        .findBySwapRequestIdOrderBySentAtAsc(testSwapRequest1.getId());
    assertEquals(2, dbMessages.size());
    assertTrue(dbMessages.get(0).isReadByReceiver()); // First message marked as read
    assertFalse(dbMessages.get(1).isReadByReceiver()); // Second message not read by sender
  }

  @Test
  void testChatConversationBetweenUsers() {
    // 1. Start conversation - sender sends first message
    ChatMessage msg1 = chatService.sendMessage(testSwapRequest1.getId(), senderUser.getId(),
        "Hello! I'm interested in your book.");

    // 2. Receiver reads messages and responds
    List<ChatMessage> messages = chatService.getChatMessages(testSwapRequest1.getId(), receiverUser.getId());
    assertEquals(1, messages.size());

    ChatMessage msg2 = chatService.sendMessage(testSwapRequest1.getId(), receiverUser.getId(),
        "Hi! Yes, it's available. What are you offering?");

    // 3. Sender continues conversation
    chatService.getChatMessages(testSwapRequest1.getId(), senderUser.getId()); // Mark previous messages as read
    ChatMessage msg3 = chatService.sendMessage(testSwapRequest1.getId(), senderUser.getId(),
        "I have a fantasy novel in excellent condition. Would that work?");

    // 4. Receiver responds with acceptance
    chatService.getChatMessages(testSwapRequest1.getId(), receiverUser.getId()); // Mark messages as read
    ChatMessage msg4 = chatService.sendMessage(testSwapRequest1.getId(), receiverUser.getId(),
        "That sounds perfect! Let's arrange the swap.");

    // 5. Verify complete conversation
    List<ChatMessage> fullConversation = chatService.getChatMessages(testSwapRequest1.getId(), senderUser.getId());
    assertEquals(4, fullConversation.size());

    // Verify message order and content
    assertEquals("Hello! I'm interested in your book.", fullConversation.get(0).getMessage());
    assertEquals("Hi! Yes, it's available. What are you offering?", fullConversation.get(1).getMessage());
    assertEquals("I have a fantasy novel in excellent condition. Would that work?",
        fullConversation.get(2).getMessage());
    assertEquals("That sounds perfect! Let's arrange the swap.", fullConversation.get(3).getMessage());

    // 6. Verify message ownership indicators
    List<ChatMessageResponse> senderView = fullConversation.stream()
        .map(msg -> new ChatMessageResponse(msg, senderUser.getId()))
        .toList();

    assertTrue(senderView.get(0).isOwnMessage()); // Sender's message
    assertFalse(senderView.get(1).isOwnMessage()); // Receiver's message
    assertTrue(senderView.get(2).isOwnMessage()); // Sender's message
    assertFalse(senderView.get(3).isOwnMessage()); // Receiver's message

    List<ChatMessageResponse> receiverView = fullConversation.stream()
        .map(msg -> new ChatMessageResponse(msg, receiverUser.getId()))
        .toList();

    assertFalse(receiverView.get(0).isOwnMessage()); // Sender's message
    assertTrue(receiverView.get(1).isOwnMessage()); // Receiver's message
    assertFalse(receiverView.get(2).isOwnMessage()); // Sender's message
    assertTrue(receiverView.get(3).isOwnMessage()); // Receiver's message

    // 7. Verify read status tracking
    assertEquals(0, chatService.getUnreadMessageCount(testSwapRequest1.getId(), receiverUser.getId()));
    assertEquals(0, chatService.getUnreadMessageCount(testSwapRequest1.getId(), senderUser.getId())); // Messages are
                                                                                                      // marked as read
                                                                                                      // when
                                                                                                      // getChatMessages
                                                                                                      // is called
  }

  @Test
  void testStatusChangeNotificationsAndUpdates() {
    // 1. Send initial message
    chatService.sendMessage(testSwapRequest1.getId(), senderUser.getId(), "Interested in swapping!");

    // 2. Receiver views and accepts request
    inboxService.markInboxItemAsRead(testSwapRequest1.getId(), receiverUser.getId());
    SwapRequest acceptedRequest = inboxService.updateSwapRequestStatus(testSwapRequest1.getId(),
        "Accepted", receiverUser.getId());

    assertEquals(SwapStatus.ACCEPTED, acceptedRequest.getSwapStatus());

    // 3. Verify status change is reflected in both users' views
    List<SwapRequest> receivedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), null, null);
    SwapRequest receiverView = receivedRequests.stream()
        .filter(r -> r.getId().equals(testSwapRequest1.getId()))
        .findFirst()
        .orElseThrow();
    assertEquals(SwapStatus.ACCEPTED, receiverView.getSwapStatus());

    List<SwapRequest> sentRequests = inboxService.getUnifiedInbox(senderUser.getId(), null, null);
    SwapRequest senderView = sentRequests.stream()
        .filter(r -> r.getId().equals(testSwapRequest1.getId()))
        .findFirst()
        .orElseThrow();
    assertEquals(SwapStatus.ACCEPTED, senderView.getSwapStatus());

    // 4. Test status filtering after change
    List<SwapRequest> acceptedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), "Accepted", null);
    assertEquals(2, acceptedRequests.size()); // testSwapRequest1 (now accepted) + testSwapRequest2 (already accepted)

    List<SwapRequest> pendingRequests = inboxService.getUnifiedInbox(receiverUser.getId(), "Pending", null);
    assertEquals(0, pendingRequests.size()); // testSwapRequest1 is no longer pending

    // 5. Test invalid status transitions
    assertThrows(IllegalArgumentException.class, () -> {
      inboxService.updateSwapRequestStatus(testSwapRequest2.getId(), "Pending", receiverUser.getId());
    });

    // 6. Test unauthorized status changes
    assertThrows(IllegalArgumentException.class, () -> {
      inboxService.updateSwapRequestStatus(testSwapRequest1.getId(), "Rejected", senderUser.getId());
    });

    // 7. Verify database consistency after status changes
    SwapRequestDao dbRequest = swapRequestRepository.findById(testSwapRequest1.getId()).orElseThrow();
    assertEquals("Accepted", dbRequest.getSwapStatus());
    assertNotNull(dbRequest.getUpdatedAt());
  }

  @Test
  void testInboxFilteringAndSortingCombinations() {
    // Add more test data for comprehensive filtering/sorting tests
    UserDao extraUser = createTestUser("Charlie", "Extra", "charlie.extra@test.com");
    BookDao extraBook = createTestBook("Zebra Book", "Zebra Author", receiverUser);
    SwapRequestDao extraRequest = createTestSwapRequest(extraUser, receiverUser, extraBook, "Pending",
        Instant.now().minusSeconds(600), "Another request");

    // 1. Test filtering by status
    List<SwapRequest> pendingRequests = inboxService.getUnifiedInbox(receiverUser.getId(), "Pending", null);
    assertEquals(2, pendingRequests.size()); // testSwapRequest1 + extraRequest

    List<SwapRequest> acceptedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), "Accepted", null);
    assertEquals(1, acceptedRequests.size()); // testSwapRequest2

    List<SwapRequest> rejectedRequests = inboxService.getUnifiedInbox(receiverUser.getId(), "Rejected", null);
    assertEquals(1, rejectedRequests.size()); // testSwapRequest3 is a sent request that was rejected

    // 2. Test sorting by date (default)
    List<SwapRequest> allRequests = inboxService.getUnifiedInbox(receiverUser.getId(), null, "date");
    assertEquals(4, allRequests.size()); // 3 received + 1 sent request from receiverUser
    // Should be ordered by most recent first
    assertTrue(allRequests.get(0).getRequestedAt().isAfter(allRequests.get(1).getRequestedAt()));
    assertTrue(allRequests.get(1).getRequestedAt().isAfter(allRequests.get(2).getRequestedAt()));

    // 3. Test sorting by book title
    List<SwapRequest> sortedByTitle = inboxService.getUnifiedInbox(receiverUser.getId(), null, "book_title");
    assertEquals(4, sortedByTitle.size()); // 3 received + 1 sent request
    assertEquals("Another Book", sortedByTitle.get(0).getBookToSwapWith().getTitle());
    assertEquals("Test Book 1", sortedByTitle.get(1).getBookToSwapWith().getTitle());
    assertEquals("Test Book 1", sortedByTitle.get(2).getBookToSwapWith().getTitle()); // This could be the sent request
    assertEquals("Zebra Book", sortedByTitle.get(3).getBookToSwapWith().getTitle());

    // 4. Test sorting by sender name
    List<SwapRequest> sortedBySender = inboxService.getUnifiedInbox(receiverUser.getId(), null, "sender_name");
    assertEquals(4, sortedBySender.size()); // 3 received + 1 sent request
    assertEquals("Bob Another",
        sortedBySender.get(0).getSender().getFirstName() + " " + sortedBySender.get(0).getSender().getLastName());
    assertEquals("Charlie Extra",
        sortedBySender.get(1).getSender().getFirstName() + " " + sortedBySender.get(1).getSender().getLastName());
    assertEquals("Jane Receiver", // This is the sent request where receiverUser is the sender
        sortedBySender.get(2).getSender().getFirstName() + " " + sortedBySender.get(2).getSender().getLastName());
    assertEquals("John Sender",
        sortedBySender.get(3).getSender().getFirstName() + " " + sortedBySender.get(3).getSender().getLastName());

    // 5. Test combined filtering and sorting
    List<SwapRequest> pendingSortedByTitle = inboxService.getUnifiedInbox(receiverUser.getId(), "Pending",
        "book_title");
    assertEquals(2, pendingSortedByTitle.size());
    assertEquals("Test Book 1", pendingSortedByTitle.get(0).getBookToSwapWith().getTitle());
    assertEquals("Zebra Book", pendingSortedByTitle.get(1).getBookToSwapWith().getTitle());

    // 6. Test inbox messages filtering and sorting
    List<SwapRequest> msgs = inboxService.getUnifiedInbox(senderUser.getId(), null, null);
    assertEquals(2, msgs.size());

    List<SwapRequest> sentRejected = inboxService.getUnifiedInbox(receiverUser.getId(), "Rejected", null);
    assertEquals(1, sentRejected.size());
    assertEquals(testSwapRequest3.getId(), sentRejected.get(0).getId());

    // 7. Test invalid filter values
    assertThrows(Exception.class, () -> {
      inboxService.getUnifiedInbox(receiverUser.getId(), "InvalidStatus", null);
    });
  }

  @Test
  void testDatabaseConsistencyAcrossOperations() {
    // 1. Initial state verification
    assertEquals(3, swapRequestRepository.count());
    assertEquals(0, chatMessageRepository.count());

    // 2. Send messages and verify database state
    ChatMessage msg1 = chatService.sendMessage(testSwapRequest1.getId(), senderUser.getId(), "Message 1");
    ChatMessage msg2 = chatService.sendMessage(testSwapRequest1.getId(), receiverUser.getId(), "Message 2");
    ChatMessage msg3 = chatService.sendMessage(testSwapRequest2.getId(), anotherUser.getId(), "Message 3");

    assertEquals(3, chatMessageRepository.count());

    // Verify message-swap request relationships
    List<ChatMessageDao> request1Messages = chatMessageRepository
        .findBySwapRequestIdOrderBySentAtAsc(testSwapRequest1.getId());
    assertEquals(2, request1Messages.size());

    List<ChatMessageDao> request2Messages = chatMessageRepository
        .findBySwapRequestIdOrderBySentAtAsc(testSwapRequest2.getId());
    assertEquals(1, request2Messages.size());

    // 3. Update swap request status and verify consistency
    SwapRequest updatedRequest = inboxService.updateSwapRequestStatus(testSwapRequest1.getId(), "Accepted",
        receiverUser.getId());

    SwapRequestDao dbRequest = swapRequestRepository.findById(testSwapRequest1.getId()).orElseThrow();
    assertEquals("Accepted", dbRequest.getSwapStatus());
    assertEquals(updatedRequest.getSwapStatus().getCode(), dbRequest.getSwapStatus());

    // 4. Mark messages as read and verify database state
    chatService.getChatMessages(testSwapRequest1.getId(), receiverUser.getId()); // Auto-marks as read

    List<ChatMessageDao> updatedMessages = chatMessageRepository
        .findBySwapRequestIdOrderBySentAtAsc(testSwapRequest1.getId());
    assertTrue(updatedMessages.get(0).isReadByReceiver()); // Sender's message marked as read
    assertFalse(updatedMessages.get(1).isReadByReceiver()); // Receiver's own message

    // 5. Mark inbox items as read and verify timestamps
    inboxService.markInboxItemAsRead(testSwapRequest1.getId(), receiverUser.getId());
    inboxService.markInboxItemAsRead(testSwapRequest1.getId(), senderUser.getId());

    SwapRequestDao readRequest = swapRequestRepository.findById(testSwapRequest1.getId()).orElseThrow();
    assertNotNull(readRequest.getReadByReceiverAt());
    assertNotNull(readRequest.getReadBySenderAt());

    // 6. Test cascade operations and referential integrity
    // Verify that swap requests reference correct users and books
    List<SwapRequestDao> allRequests = swapRequestRepository.findAll();
    for (SwapRequestDao request : allRequests) {
      assertNotNull(request.getSender());
      assertNotNull(request.getReceiver());
      assertNotNull(request.getBookToSwapWith());
      assertTrue(userRepository.existsById(request.getSender().getId()));
      assertTrue(userRepository.existsById(request.getReceiver().getId()));
      assertTrue(bookRepository.existsById(request.getBookToSwapWith().getId()));
    }

    // Verify that chat messages reference correct users and swap requests
    List<ChatMessageDao> allMessages = chatMessageRepository.findAll();
    for (ChatMessageDao message : allMessages) {
      assertNotNull(message.getSender());
      assertNotNull(message.getSwapRequestId());
      assertTrue(userRepository.existsById(message.getSender().getId()));
      assertTrue(swapRequestRepository.existsById(message.getSwapRequestId()));
    }

    // 7. Test transaction consistency
    // All operations should be atomic - if one fails, all should rollback
    String invalidUserId = "nonexistent";
    assertThrows(Exception.class, () -> {
      chatService.sendMessage(testSwapRequest1.getId(), invalidUserId, "This should fail");
    });

    // Verify database state hasn't changed after failed operation
    assertEquals(3, chatMessageRepository.count()); // Still 3 messages

    // 8. Test unread count consistency
    long unreadCount1 = chatService.getUnreadMessageCount(testSwapRequest1.getId(), receiverUser.getId());
    long unreadCount2 = inboxService.getUnreadMessageCount(receiverUser.getId(), testSwapRequest1.getId());
    assertEquals(unreadCount1, unreadCount2); // Both methods should return same count
  }

  // Helper methods
  private UserDao createTestUser(String firstName, String lastName, String email) {
    UserDao user = new UserDao();
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmail(email);
    user.setPassword("password");
    user.setEmailVerified(true);
    user.setSalt("salt");
    return userRepository.save(user);
  }

  private BookDao createTestBook(String title, String author, UserDao owner) {
    BookDao book = new BookDao();
    book.setTitle(title);
    book.setAuthor(author);
    book.setCondition("Good");
    book.setLanguage("English");
    book.setOwner(owner);
    book.setCoverPhotos(List.of());
    book.setGenres(List.of());
    book.setSwapCondition(createTestSwapCondition());
    return bookRepository.save(book);
  }

  private SwapConditionDao createTestSwapCondition() {
    SwapConditionDao dao = new SwapConditionDao();
    dao.setSwapType("GiveAway");
    dao.setGiveAway(true);
    return dao;
  }

  private SwapRequestDao createTestSwapRequest(UserDao sender, UserDao receiver, BookDao book,
      String status, Instant requestedAt, String note) {
    SwapRequestDao request = new SwapRequestDao();
    request.setSender(sender);
    request.setReceiver(receiver);
    request.setBookToSwapWith(book);
    request.setSwapType("ByBooks");
    request.setSwapStatus(status);
    request.setAskForGiveaway(false);
    request.setNote(note);
    request.setRequestedAt(requestedAt);
    request.setUpdatedAt(requestedAt);
    return swapRequestRepository.save(request);
  }
}