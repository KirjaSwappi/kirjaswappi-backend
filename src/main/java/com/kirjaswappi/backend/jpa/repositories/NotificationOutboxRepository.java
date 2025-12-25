/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.kirjaswappi.backend.jpa.daos.NotificationOutboxDao;

@Repository
public interface NotificationOutboxRepository extends MongoRepository<NotificationOutboxDao, String> {
  // Find pending notifications created before a certain time (for processing)
  // or just all pending.
  List<NotificationOutboxDao> findByStatus(String status);

  // Optionally find by status and sort by creation time
  List<NotificationOutboxDao> findByStatusOrderByCreatedAtAsc(String status);
}
