/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import java.time.Instant;

import lombok.NoArgsConstructor;

import org.springframework.stereotype.Component;

import com.kirjaswappi.backend.jpa.daos.SwapRequestDao;
import com.kirjaswappi.backend.service.entities.*;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.enums.SwapType;

@Component
@NoArgsConstructor
public class SwapRequestMapper {
  public static SwapRequest toEntity(SwapRequestDao dao) {
    var entity = new SwapRequest();
    entity.setId(dao.getId());
    entity.setSender(UserMapper.toEntity(dao.getSender()));
    entity.setReceiver(UserMapper.toEntity(dao.getReceiver()));
    entity.setBookToSwapWith(BookMapper.toEntity(dao.getBookToSwapWith()));
    entity.setSwapType(SwapType.fromCode(dao.getSwapType()));
    if (dao.getSwapOfferDao() != null) {
      entity.setSwapOffer(SwapOfferMapper.toEntity(dao.getSwapOfferDao()));
    }
    entity.setAskForGiveaway(dao.isAskForGiveaway());
    entity.setSwapStatus(SwapStatus.fromCode(dao.getSwapStatus()));
    entity.setNote(dao.getNote());
    entity.setRequestedAt(dao.getRequestedAt());
    entity.setUpdatedAt(dao.getUpdatedAt());
    entity.setReadByReceiverAt(dao.getReadByReceiverAt());
    entity.setReadBySenderAt(dao.getReadBySenderAt());
    return entity;
  }

  public static SwapRequestDao toDao(SwapRequest entity) {
    var dao = new SwapRequestDao();
    dao.setId(entity.getId());
    dao.setSender(UserMapper.toDao(entity.getSender()));
    dao.setReceiver(UserMapper.toDao(entity.getReceiver()));
    dao.setBookToSwapWith(BookMapper.toDao(entity.getBookToSwapWith()));
    dao.setSwapType(entity.getSwapType().getCode());
    if (entity.getSwapOffer() != null) {
      dao.setSwapOfferDao(SwapOfferMapper.toDao(entity.getSwapOffer()));
    }
    dao.setAskForGiveaway(entity.isAskForGiveaway());
    dao.setSwapStatus(entity.getSwapStatus().getCode());
    dao.setNote(entity.getNote());
    var currentTime = Instant.now();
    dao.setRequestedAt(entity.getRequestedAt() == null ? currentTime : entity.getRequestedAt());
    dao.setUpdatedAt(entity.getUpdatedAt() == null ? currentTime : entity.getUpdatedAt());
    dao.setReadByReceiverAt(entity.getReadByReceiverAt());
    dao.setReadBySenderAt(entity.getReadBySenderAt());
    return dao;
  }
}
