/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.result.UpdateResult;

@ChangeUnit(id = "add-version-field-to-users", order = "006", author = "mahiuddinalkamal")
public class AddVersionFieldToUsers {

  private final MongoTemplate mongoTemplate;

  public AddVersionFieldToUsers(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Execution
  public void executeMigration() {
    // Initialise @Version field on all existing user documents that don't have it.
    // Without this, Spring Data treats version=null as a new entity and attempts
    // insert instead of update, causing E11000 duplicate _id errors.
    UpdateResult result = mongoTemplate.getCollection("users").updateMany(
        new Document("version", new Document("$exists", false)),
        new Document("$set", new Document("version", 0L)));
    System.out.printf("[AddVersionFieldToUsers] Initialised version=0 on %d user document(s)%n",
        result.getModifiedCount());
  }

  @RollbackExecution
  public void rollbackMigration() {
    mongoTemplate.getCollection("users").updateMany(
        new Document(),
        new Document("$unset", new Document("version", "")));
  }
}
