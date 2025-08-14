/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;

public interface ChatMessageRepository extends MongoRepository<ChatMessageDao, String> {
  List<ChatMessageDao> findBySwapRequestIdOrderBySentAtAsc(String swapRequestId);

  long countBySwapRequestIdAndSenderIdNotAndReadByReceiverFalse(String swapRequestId, String senderId);
}