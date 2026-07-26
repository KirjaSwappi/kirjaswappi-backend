/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.result.UpdateResult;

@ChangeUnit(id = "add-version-field-to-users", order = "006", author = "mahiuddinalkamal")
public class AddVersionFieldToUsers {

  private static final Logger logger = LoggerFactory.getLogger(AddVersionFieldToUsers.class);

  private final MongoTemplate mongoTemplate;

  public AddVersionFieldToUsers(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Execution
  public void executeMigration() {
    // Initialise @Version field on all existing user documents that don't have it.
    // Without this, Spring Data treats version=null as a new entity and attempts
    // insert instead of update, causing E11000 duplicate _id errors.
    UpdateResult result = mongoTemplate
        .getCollection("users")
        .updateMany(
            new Document("version", new Document("$exists", false)),
            new Document("$set", new Document("version", 0L)));
    logger.info("Initialised version=0 on {} user document(s)", result.getModifiedCount());
  }

  @RollbackExecution
  public void rollbackMigration() {
    // Only unset version on documents that were set to exactly 0 by this migration.
    // Documents with a higher version were already incremented by the application
    // and should not be touched.
    mongoTemplate
        .getCollection("users")
        .updateMany(
            new Document("version", 0L),
            new Document("$unset", new Document("version", "")));
  }
}
