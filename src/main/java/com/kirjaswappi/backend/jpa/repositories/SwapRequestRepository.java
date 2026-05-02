/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;

public interface SwapRequestRepository extends MongoRepository<SwapRequestDao, String> {
  /**
   * Returns true only if there is an *active* swap (PENDING/ACCEPTED/RESERVED)
   * for this triple. Closed states (REJECTED/CANCELLED/EXPIRED/COMPLETED) must
   * not block the same pair from re-requesting the book later.
   */
  @Query(value = "{ 'sender.$id': ?0, 'receiver.$id': ?1, 'bookToSwapWith.$id': ?2, "
      + "'swapStatus': { $in: ['Pending', 'Accepted', 'Reserved'] } }", exists = true)
  boolean existsAlready(ObjectId senderId, ObjectId receiverId, ObjectId bookToSwapWithId);

  // Inbox query methods for received swap requests
  List<SwapRequestDao> findByReceiverIdOrderByRequestedAtDesc(String receiverId);

  // Inbox query methods for sent swap requests
  List<SwapRequestDao> findBySenderIdOrderByRequestedAtDesc(String senderId);

  // Inbox query methods with status filtering for received requests
  List<SwapRequestDao> findByReceiverIdAndSwapStatusOrderByRequestedAtDesc(String receiverId, String swapStatus);

  // Inbox query methods with status filtering for sent requests
  List<SwapRequestDao> findBySenderIdAndSwapStatusOrderByRequestedAtDesc(String senderId, String swapStatus);

  // Find all swap requests for a specific book
  List<SwapRequestDao> findByBookToSwapWithId(String bookToSwapWithId);

  // Find active swap requests for a specific book
  List<SwapRequestDao> findByBookToSwapWithIdAndSwapStatus(String bookToSwapWithId, String swapStatus);

  void deleteAllBySenderId(String senderId);

  void deleteAllByReceiverId(String receiverId);
}
