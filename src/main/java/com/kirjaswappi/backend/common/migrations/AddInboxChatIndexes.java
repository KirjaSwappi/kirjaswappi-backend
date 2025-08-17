/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@ChangeUnit(id = "add-inbox-chat-indexes", order = "004", author = "mahiuddinalkamal")
public class AddInboxChatIndexes {

  private final MongoTemplate mongoTemplate;

  public AddInboxChatIndexes(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Execution
  public void executeMigration() {
    // Index on chat_messages collection for swapRequestId and sentAt
    mongoTemplate.indexOps("chat_messages")
        .createIndex(new Index()
            .on("swapRequestId", Sort.Direction.ASC)
            .on("sentAt", Sort.Direction.ASC)
            .named("idx_chat_messages_swap_request_sent_at"));

    // Compound index for unread message counting queries
    mongoTemplate.indexOps("chat_messages")
        .createIndex(new Index()
            .on("swapRequestId", Sort.Direction.ASC)
            .on("sender.$id", Sort.Direction.ASC)
            .on("readByReceiver", Sort.Direction.ASC)
            .named("idx_chat_messages_unread_count"));

    // Index on swap_requests for inbox filtering queries - receiver and requestedAt
    mongoTemplate.indexOps("swap_requests")
        .createIndex(new Index()
            .on("receiver.$id", Sort.Direction.ASC)
            .on("requestedAt", Sort.Direction.DESC)
            .named("idx_swap_requests_receiver_requested_at"));

    // Index on swap_requests for inbox filtering queries - sender and requestedAt
    mongoTemplate.indexOps("swap_requests")
        .createIndex(new Index()
            .on("sender.$id", Sort.Direction.ASC)
            .on("requestedAt", Sort.Direction.DESC)
            .named("idx_swap_requests_sender_requested_at"));

    // Compound index for swap_requests filtering by receiver, status and
    // requestedAt
    mongoTemplate.indexOps("swap_requests")
        .createIndex(new Index()
            .on("receiver.$id", Sort.Direction.ASC)
            .on("swapStatus", Sort.Direction.ASC)
            .on("requestedAt", Sort.Direction.DESC)
            .named("idx_swap_requests_receiver_status_requested_at"));

    // Compound index for swap_requests filtering by sender, status and requestedAt
    mongoTemplate.indexOps("swap_requests")
        .createIndex(new Index()
            .on("sender.$id", Sort.Direction.ASC)
            .on("swapStatus", Sort.Direction.ASC)
            .on("requestedAt", Sort.Direction.DESC)
            .named("idx_swap_requests_sender_status_requested_at"));
  }

  @RollbackExecution
  public void rollbackMigration() {
    // Drop the indexes in reverse order
    mongoTemplate.indexOps("swap_requests").dropIndex("idx_swap_requests_sender_status_requested_at");
    mongoTemplate.indexOps("swap_requests").dropIndex("idx_swap_requests_receiver_status_requested_at");
    mongoTemplate.indexOps("swap_requests").dropIndex("idx_swap_requests_sender_requested_at");
    mongoTemplate.indexOps("swap_requests").dropIndex("idx_swap_requests_receiver_requested_at");
    mongoTemplate.indexOps("chat_messages").dropIndex("idx_chat_messages_unread_count");
    mongoTemplate.indexOps("chat_messages").dropIndex("idx_chat_messages_swap_request_sent_at");
  }
}