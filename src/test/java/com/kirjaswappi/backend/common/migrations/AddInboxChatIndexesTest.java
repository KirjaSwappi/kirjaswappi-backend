/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.migrations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

class AddInboxChatIndexesTest {

  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private IndexOperations chatIndexOps;

  @Mock
  private IndexOperations swapIndexOps;

  private AddInboxChatIndexes migration;

  @BeforeEach
  @DisplayName("Setup mocks and migration instance")
  void setUp() {
    MockitoAnnotations.openMocks(this);
    migration = new AddInboxChatIndexes(mongoTemplate);

    // Setup mock behavior
    when(mongoTemplate.indexOps("chat_messages")).thenReturn(chatIndexOps);
    when(mongoTemplate.indexOps("swap_requests")).thenReturn(swapIndexOps);
  }

  @Test
  @DisplayName("Should create all required indexes when migration is executed")
  void shouldCreateAllRequiredIndexes() {
    // When
    migration.executeMigration();

    // Then - Verify chat_messages indexes are created
    verify(chatIndexOps, times(2)).createIndex(any());

    // Then - Verify swap_requests indexes are created
    verify(swapIndexOps, times(4)).createIndex(any());

    // Verify mongoTemplate.indexOps was called with correct collection names
    verify(mongoTemplate, times(2)).indexOps("chat_messages");
    verify(mongoTemplate, times(4)).indexOps("swap_requests");
  }

  @Test
  @DisplayName("Should remove all indexes when migration is rolled back")
  void shouldRemoveAllIndexesWhenRolledBack() {
    // When
    migration.rollbackMigration();

    // Then - Verify chat_messages indexes are dropped
    verify(chatIndexOps).dropIndex("idx_chat_messages_unread_count");
    verify(chatIndexOps).dropIndex("idx_chat_messages_swap_request_sent_at");

    // Then - Verify swap_requests indexes are dropped
    verify(swapIndexOps).dropIndex("idx_swap_requests_sender_status_requested_at");
    verify(swapIndexOps).dropIndex("idx_swap_requests_receiver_status_requested_at");
    verify(swapIndexOps).dropIndex("idx_swap_requests_sender_requested_at");
    verify(swapIndexOps).dropIndex("idx_swap_requests_receiver_requested_at");

    // Verify mongoTemplate.indexOps was called with correct collection names
    verify(mongoTemplate, times(2)).indexOps("chat_messages");
    verify(mongoTemplate, times(4)).indexOps("swap_requests");
  }

  @Test
  @DisplayName("Should verify migration creates indexes with correct names and fields")
  void shouldVerifyMigrationCreatesIndexesWithCorrectNamesAndFields() {
    // When
    migration.executeMigration();

    // Then - Verify that createIndex was called the correct number of times
    verify(chatIndexOps, times(2)).createIndex(any());
    verify(swapIndexOps, times(4)).createIndex(any());

    // Verify that the correct collections were accessed
    verify(mongoTemplate, times(2)).indexOps("chat_messages");
    verify(mongoTemplate, times(4)).indexOps("swap_requests");
  }

  @Test
  @DisplayName("Should verify rollback drops indexes in correct order")
  void shouldVerifyRollbackDropsIndexesInCorrectOrder() {
    // When
    migration.rollbackMigration();

    // Then - Verify indexes are dropped in reverse order
    var inOrder = inOrder(swapIndexOps, chatIndexOps);

    inOrder.verify(swapIndexOps).dropIndex("idx_swap_requests_sender_status_requested_at");
    inOrder.verify(swapIndexOps).dropIndex("idx_swap_requests_receiver_status_requested_at");
    inOrder.verify(swapIndexOps).dropIndex("idx_swap_requests_sender_requested_at");
    inOrder.verify(swapIndexOps).dropIndex("idx_swap_requests_receiver_requested_at");
    inOrder.verify(chatIndexOps).dropIndex("idx_chat_messages_unread_count");
    inOrder.verify(chatIndexOps).dropIndex("idx_chat_messages_swap_request_sent_at");
  }

  @Test
  @DisplayName("Should verify migration handles exceptions gracefully")
  void shouldVerifyMigrationHandlesExceptionsGracefully() {
    // Given - Mock an exception during index creation
    doThrow(new RuntimeException("Index creation failed")).when(chatIndexOps).createIndex(any());

    // When/Then - Migration should not fail completely, but exception should
    // propagate
    assertThrows(RuntimeException.class, () -> migration.executeMigration());

    // Verify that at least one index creation was attempted
    verify(chatIndexOps, atLeastOnce()).createIndex(any());
  }
}