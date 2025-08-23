/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;

public interface ChatMessageRepository extends MongoRepository<ChatMessageDao, String> {
  List<ChatMessageDao> findBySwapRequestIdOrderBySentAtAsc(String swapRequestId);

  @Query(value = "{ 'swapRequestId': ?0, 'readByReceiver': false, 'sender.$id': { $ne: { $oid: ?1 } } }", count = true)
  long countBySwapRequestIdAndReadByReceiverFalseAndSenderIdNot(String swapRequestId, String userId);

  Optional<ChatMessageDao> findFirstBySwapRequestIdOrderBySentAtDesc(String swapRequestId);
}
