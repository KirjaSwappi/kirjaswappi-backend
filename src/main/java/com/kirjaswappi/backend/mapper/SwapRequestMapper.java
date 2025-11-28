/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import java.time.Instant;

import org.jspecify.annotations.NullMarked;

import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;

@NullMarked // Meaning every params in this class is by default expected to be not null
public final class SwapRequestMapper {

  private SwapRequestMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static SwapRequest toEntity(SwapRequestDao dao) {
    return SwapRequest.builder()
        .id(dao.id())
        .sender(UserMapper.toEntity(dao.sender()))
        .receiver(UserMapper.toEntity(dao.receiver()))
        .bookToSwapWith(BookMapper.toEntity(dao.bookToSwapWith()))
        .swapType(SwapType.fromCode(dao.swapType()))
        .swapOffer(dao.swapOfferDao() != null ? SwapOfferMapper.toEntity(dao.swapOfferDao()) : null)
        .askForGiveaway(dao.askForGiveaway())
        .swapStatus(SwapStatus.fromCode(dao.swapStatus()))
        .note(dao.note())
        .requestedAt(dao.requestedAt())
        .updatedAt(dao.updatedAt())
        .readByReceiverAt(dao.readByReceiverAt())
        .readBySenderAt(dao.readBySenderAt())
        .build();
  }

  public static SwapRequestDao toDao(SwapRequest entity) {
    return SwapRequestDao.builder()
        .id(entity.id())
        .sender(UserMapper.toDao(entity.sender()))
        .receiver(UserMapper.toDao(entity.receiver()))
        .bookToSwapWith(BookMapper.toDao(entity.bookToSwapWith()))
        .swapType(entity.swapType().getCode())
        .swapOfferDao(entity.swapOffer() != null ? SwapOfferMapper.toDao(entity.swapOffer()) : null)
        .askForGiveaway(entity.askForGiveaway())
        .swapStatus(entity.swapStatus().getCode())
        .note(entity.note())
        .requestedAt(entity.requestedAt() == null ? Instant.now() : entity.requestedAt())
        .updatedAt(entity.updatedAt() == null ? Instant.now() : entity.updatedAt())
        .readByReceiverAt(entity.readByReceiverAt())
        .readBySenderAt(entity.readBySenderAt())
        .build();
  }
}
