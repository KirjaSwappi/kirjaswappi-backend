/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class CustomChatMessageRepositoryImpl implements CustomChatMessageRepository {

  private final MongoTemplate mongoTemplate;

  public CustomChatMessageRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public long markAsRead(String swapRequestId, String userId) {
    Query query = new Query(Criteria.where("swapRequestId").is(swapRequestId)
        .and("readByReceiver").is(false)
        .and("sender.$id").ne(new ObjectId(userId)));
    Update update = new Update().set("readByReceiver", true);
    return mongoTemplate.updateMulti(query, update, "chatMessages").getModifiedCount();
  }
}
