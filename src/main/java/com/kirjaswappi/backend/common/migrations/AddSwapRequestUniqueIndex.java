/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.migrations;

import java.util.List;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "add-swap-request-unique-index", order = "005", author = "mahiuddinalkamal")
public class AddSwapRequestUniqueIndex {

  private final MongoTemplate mongoTemplate;

  public AddSwapRequestUniqueIndex(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Execution
  public void executeMigration() {
    mongoTemplate.execute("swap_requests", collection -> {
      org.bson.Document keys = new org.bson.Document("sender.$id", 1)
          .append("receiver.$id", 1)
          .append("bookToSwapWith.$id", 1);

      org.bson.Document partialFilter = new org.bson.Document("swapStatus",
          new org.bson.Document("$in", List.of("Pending", "Accepted", "Reserved")));

      com.mongodb.client.model.IndexOptions options = new com.mongodb.client.model.IndexOptions()
          .unique(true)
          .partialFilterExpression(partialFilter)
          .name("swap_request_active_unique_idx");

      collection.createIndex(keys, options);
      return null;
    });
  }

  @RollbackExecution
  public void rollbackMigration() {
    mongoTemplate.indexOps("swap_requests").dropIndex("swap_request_active_unique_idx");
  }
}
