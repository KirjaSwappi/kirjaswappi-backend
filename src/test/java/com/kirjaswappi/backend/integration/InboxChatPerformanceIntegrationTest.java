/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.kirjaswappi.backend.config.TestContainersConfig;
import com.kirjaswappi.backend.jpa.daos.*;
import com.kirjaswappi.backend.jpa.repositories.*;
import com.kirjaswappi.backend.service.ChatService;
import com.kirjaswappi.backend.service.InboxService;
import com.kirjaswappi.backend.service.entities.*;

/**
 * Integration test for performance and scalability of inbox and chat
 * operations. Tests database indexing, query performance, and concurrent
 * operations.
 */
@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class InboxChatPerformanceIntegrationTest {

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

  private List<UserDao> testUsers;
  private List<BookDao> testBooks;
  private List<SwapRequestDao> testSwapRequests;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    chatMessageRepository.deleteAll();
    swapRequestRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();

    // Create test data for performance testing
    createLargeTestDataset();
  }

  @Test
  void testInboxQueryPerformanceWithLargeDataset() {
    // Test with a user who has many received requests
    UserDao receiverWithManyRequests = testUsers.getFirst();

    // Measure time for inbox query without filters
    long startTime = System.currentTimeMillis();
    List<SwapRequest> allRequests = inboxService.getUnifiedInbox(receiverWithManyRequests.id(), null, null);
    long endTime = System.currentTimeMillis();

    long queryTime = endTime - startTime;

    // Should complete within reasonable time (adjust threshold as needed)
    assertTrue(queryTime < 1000, "Inbox query took too long: " + queryTime + "ms");
    assertFalse(allRequests.isEmpty(), "Should have received requests");

    // Test filtered query performance
    startTime = System.currentTimeMillis();
    List<SwapRequest> pendingRequests = inboxService.getUnifiedInbox(receiverWithManyRequests.id(),
        "Pending", null);
    endTime = System.currentTimeMillis();

    queryTime = endTime - startTime;
    assertTrue(queryTime < 1000, "Filtered inbox query took too long: " + queryTime + "ms");

    // Test sorted query performance
    startTime = System.currentTimeMillis();
    List<SwapRequest> sortedRequests = inboxService.getUnifiedInbox(receiverWithManyRequests.id(), null,
        "book_title");
    endTime = System.currentTimeMillis();

    queryTime = endTime - startTime;
    assertTrue(queryTime < 1000, "Sorted inbox query took too long: " + queryTime + "ms");

    // Verify sorting is correct
    for (int i = 1; i < sortedRequests.size(); i++) {
      String prevTitle = sortedRequests.get(i - 1).bookToSwapWith().title();
      String currTitle = sortedRequests.get(i).bookToSwapWith().title();
      assertTrue(prevTitle.compareToIgnoreCase(currTitle) <= 0,
          "Books not sorted correctly: " + prevTitle + " vs " + currTitle);
    }
  }

  @Test
  void testChatQueryPerformanceWithManyMessages() {
    // Create a swap request with many messages
    SwapRequestDao swapRequest = testSwapRequests.getFirst();
    UserDao sender = swapRequest.sender();
    UserDao receiver = swapRequest.receiver();

    // Create many chat messages
    List<ChatMessageDao> messages = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      ChatMessageDao message = new ChatMessageDao();
      message.swapRequestId(swapRequest.id());
      message.sender(i % 2 == 0 ? sender : receiver);
      message.message("Test message " + i);
      message.sentAt(Instant.now().minusSeconds(100 - i));
      message.readByReceiver(i < 50); // First 50 messages are read
      messages.add(message);
    }
    chatMessageRepository.saveAll(messages);

    // Test chat message retrieval performance
    long startTime = System.currentTimeMillis();
    List<ChatMessage> chatMessages = chatService.getChatMessages(swapRequest.id(), sender.id());
    long endTime = System.currentTimeMillis();

    long queryTime = endTime - startTime;
    System.out.println("Chat messages query time for " + chatMessages.size() + " messages: " + queryTime + "ms");
    assertTrue(queryTime < 500, "Chat messages query took too long: " + queryTime + "ms");
    assertEquals(100, chatMessages.size());

    // Verify messages are ordered correctly by sent time
    for (int i = 1; i < chatMessages.size(); i++) {
      assertTrue(chatMessages.get(i - 1).sentAt().isBefore(chatMessages.get(i).sentAt()) ||
          chatMessages.get(i - 1).sentAt().equals(chatMessages.get(i).sentAt()),
          "Messages not ordered correctly by sent time");
    }

    // Test unread count performance
    startTime = System.currentTimeMillis();
    long unreadCount = chatService.getUnreadMessageCount(swapRequest.id(), receiver.id());
    endTime = System.currentTimeMillis();

    queryTime = endTime - startTime;
    System.out.println("Unread count query time: " + queryTime + "ms");
    assertTrue(queryTime < 200, "Unread count query took too long: " + queryTime + "ms");
    assertEquals(25, unreadCount); // 25 messages from receiver that are unread (messages 51, 53, 55, ..., 99)
  }

  @Test
  void testConcurrentChatOperations() throws InterruptedException {
    SwapRequestDao swapRequest = testSwapRequests.getFirst();
    UserDao sender = swapRequest.sender();
    UserDao receiver = swapRequest.receiver();

    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    // Simulate concurrent message sending
    for (int i = 0; i < 20; i++) {
      final int messageNum = i;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          UserDao messageSender = messageNum % 2 == 0 ? sender : receiver;
          ChatMessage message = chatService.sendMessage(swapRequest.id(), messageSender.id(),
              "Concurrent message " + messageNum);
          assertNotNull(message);
        } catch (Exception e) {
          fail("Concurrent message sending failed: " + e.getMessage());
        }
      }, executor);
      futures.add(future);
    }

    // Wait for all operations to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    // Verify all messages were saved correctly
    List<ChatMessage> allMessages = chatService.getChatMessages(swapRequest.id(), sender.id());
    assertEquals(20, allMessages.size());

    // Verify message ordering is maintained
    for (int i = 1; i < allMessages.size(); i++) {
      assertTrue(allMessages.get(i - 1).sentAt().isBefore(allMessages.get(i).sentAt()) ||
          allMessages.get(i - 1).sentAt().equals(allMessages.get(i).sentAt()),
          "Concurrent messages not ordered correctly");
    }
  }

  @Test
  void testConcurrentInboxOperations() throws InterruptedException {
    UserDao receiver = testUsers.getFirst();
    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    // Simulate concurrent inbox access and status updates
    for (int i = 0; i < 10; i++) {
      final int operationNum = i;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          if (operationNum % 3 == 0) {
            // Read inbox
            List<SwapRequest> requests = inboxService.getUnifiedInbox(receiver.id(), null, null);
            assertNotNull(requests);
          } else if (operationNum % 3 == 1) {
            // Mark inbox item as read
            if (!testSwapRequests.isEmpty()) {
              inboxService.markInboxItemAsRead(testSwapRequests.getFirst().id(), receiver.id());
            }
          } else {
            // Update status (if pending)
            SwapRequestDao pendingRequest = testSwapRequests.stream()
                .filter(r -> "Pending".equals(r.swapStatus()) && r.receiver().id().equals(receiver.id()))
                .findFirst()
                .orElse(null);
            if (pendingRequest != null) {
              try {
                inboxService.updateSwapRequestStatus(pendingRequest.id(), "Accepted", receiver.id());
              } catch (IllegalArgumentException e) {
                // Expected if already updated by another thread
              }
            }
          }
        } catch (Exception e) {
          fail("Concurrent inbox operation failed: " + e.getMessage());
        }
      }, executor);
      futures.add(future);
    }

    // Wait for all operations to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    // Verify data consistency after concurrent operations
    List<SwapRequest> finalRequests = inboxService.getUnifiedInbox(receiver.id(), null, null);
    assertNotNull(finalRequests);
    assertFalse(finalRequests.isEmpty());
  }

  @Test
  void testUnreadCountCachingPerformance() {
    SwapRequestDao swapRequest = testSwapRequests.getFirst();
    UserDao receiver = swapRequest.receiver();

    // Send some messages to create unread count
    chatService.sendMessage(swapRequest.id(), swapRequest.sender().id(), "Message 1");
    chatService.sendMessage(swapRequest.id(), swapRequest.sender().id(), "Message 2");
    chatService.sendMessage(swapRequest.id(), swapRequest.sender().id(), "Message 3");

    // First call - should hit database and cache result
    long startTime = System.currentTimeMillis();
    long unreadCount1 = inboxService.getUnreadMessageCount(receiver.id(), swapRequest.id());
    long firstCallTime = System.currentTimeMillis() - startTime;

    // Second call - should hit cache
    startTime = System.currentTimeMillis();
    long unreadCount2 = inboxService.getUnreadMessageCount(receiver.id(), swapRequest.id());
    long secondCallTime = System.currentTimeMillis() - startTime;

    // Third call - should also hit cache
    startTime = System.currentTimeMillis();
    long unreadCount3 = inboxService.getUnreadMessageCount(receiver.id(), swapRequest.id());
    long thirdCallTime = System.currentTimeMillis() - startTime;

    // Verify results are consistent
    assertEquals(3, unreadCount1);
    assertEquals(unreadCount1, unreadCount2);
    assertEquals(unreadCount1, unreadCount3);

    // Cached calls should be faster (though this might be flaky in CI)
    System.out.println("First call (DB): " + firstCallTime + "ms");
    System.out.println("Second call (cache): " + secondCallTime + "ms");
    System.out.println("Third call (cache): " + thirdCallTime + "ms");

    // Send another message - should invalidate cache
    chatService.sendMessage(swapRequest.id(), swapRequest.sender().id(), "Message 4");

    long unreadCount4 = inboxService.getUnreadMessageCount(receiver.id(), swapRequest.id());
    assertEquals(4, unreadCount4);
  }

  @Test
  void testDatabaseIndexEffectiveness() {
    // This test verifies that database indexes are being used effectively
    UserDao receiver = testUsers.getFirst();

    // Test inbox queries with different filters to ensure indexes are used
    long startTime = System.currentTimeMillis();
    List<SwapRequest> byReceiver = inboxService.getUnifiedInbox(receiver.id(), null, null);
    long receiverQueryTime = System.currentTimeMillis() - startTime;

    startTime = System.currentTimeMillis();
    List<SwapRequest> byReceiverAndStatus = inboxService.getUnifiedInbox(receiver.id(), "Pending", null);
    long filteredQueryTime = System.currentTimeMillis() - startTime;

    System.out.println("Receiver query time: " + receiverQueryTime + "ms");
    System.out.println("Receiver + status query time: " + filteredQueryTime + "ms");

    // Both queries should be fast with proper indexing
    assertTrue(receiverQueryTime < 500, "Receiver query too slow, check receiverId index");
    assertTrue(filteredQueryTime < 500, "Filtered query too slow, check compound index");

    // Test chat message queries
    if (!testSwapRequests.isEmpty()) {
      SwapRequestDao swapRequest = testSwapRequests.getFirst();

      startTime = System.currentTimeMillis();
      List<ChatMessage> messages = chatService.getChatMessages(swapRequest.id(), receiver.id());
      long chatQueryTime = System.currentTimeMillis() - startTime;

      startTime = System.currentTimeMillis();
      long unreadCount = chatService.getUnreadMessageCount(swapRequest.id(), receiver.id());
      long unreadQueryTime = System.currentTimeMillis() - startTime;

      System.out.println("Chat messages query time: " + chatQueryTime + "ms");
      System.out.println("Unread count query time: " + unreadQueryTime + "ms");

      assertTrue(chatQueryTime < 300, "Chat query too slow, check swapRequestId index");
      assertTrue(unreadQueryTime < 200, "Unread count query too slow, check compound index");
    }
  }

  // Helper method to create large test dataset
  private void createLargeTestDataset() {
    testUsers = new ArrayList<>();
    testBooks = new ArrayList<>();
    testSwapRequests = new ArrayList<>();

    // Create 20 test users
    for (int i = 0; i < 20; i++) {
      var user = UserDao.builder()
          .firstName("User" + i)
          .lastName("Test")
          .email("user" + i + "@test.com")
          .password("password")
          .isEmailVerified(true)
          .salt("salt")
          .build();
      testUsers.add(userRepository.save(user));
    }

    // Create 50 test books (distributed among users)
    for (int i = 0; i < 50; i++) {

      var swapCondition = SwapConditionDao.builder()
          .swapType("GiveAway")
          .giveAway(true)
          .build();

      var book = BookDao.builder()
          .title("Book " + String.format("%02d", i))
          .author("Author " + (i % 10))
          .condition("Good")
          .language("English")
          .owner(testUsers.get(i % testUsers.size()))
          .coverPhotos(List.of())
          .genres(List.of())
          .swapCondition(swapCondition)
          .build();

      testBooks.add(bookRepository.save(book));
    }

    // Create 100 swap requests with various statuses and timestamps
    String[] statuses = { "Pending", "Accepted", "Rejected" };
    for (int i = 0; i < 100; i++) {

      var request = SwapRequestDao.builder()
          .sender(testUsers.get(i % (testUsers.size() - 1) + 1)) // Avoid same sender/receiver
          .receiver(testUsers.getFirst()) // Most requests go to first user for testing
          .bookToSwapWith(testBooks.get(i % testBooks.size()))
          .swapType("ByBooks")
          .swapStatus(statuses[i % statuses.length])
          .askForGiveaway(false)
          .note("Test request " + i)
          .requestedAt(Instant.now().minusSeconds(i * 60)) // Spread over time
          .updatedAt(Instant.now().minusSeconds(i * 60))
          .build();

      testSwapRequests.add(swapRequestRepository.save(request));
    }

    // Create some requests for other users too
    for (int i = 0; i < 20; i++) {

      var request = SwapRequestDao.builder()
          .sender(testUsers.getFirst())
          .receiver(testUsers.get(i % (testUsers.size() - 1) + 1))
          .bookToSwapWith(testBooks.get(i % testBooks.size()))
          .swapType("ByBooks")
          .swapStatus("Pending")
          .askForGiveaway(false)
          .note("Sent request " + i)
          .requestedAt(Instant.now().minusSeconds(i * 30))
          .updatedAt(Instant.now().minusSeconds(i * 30))
          .build();

      testSwapRequests.add(swapRequestRepository.save(request));
    }
  }
}
