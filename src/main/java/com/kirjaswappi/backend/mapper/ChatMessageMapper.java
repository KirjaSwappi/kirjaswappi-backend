/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import java.time.Instant;
import java.util.List;

import lombok.NoArgsConstructor;

import org.springframework.stereotype.Component;

import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.service.entities.ChatMessage;

@Component
@NoArgsConstructor
public class ChatMessageMapper {
  public static ChatMessage toEntity(ChatMessageDao dao) {
    var entity = new ChatMessage();
    entity.setId(dao.getId());
    entity.setSwapRequestId(dao.getSwapRequestId());
    entity.setSender(UserMapper.toEntity(dao.getSender()));
    entity.setMessage(dao.getMessage());
    entity.setImageIds(dao.getImageIds()); // Map image IDs
    entity.setSentAt(dao.getSentAt());
    entity.setReadByReceiver(dao.isReadByReceiver());
    return entity;
  }

  public static ChatMessage toEntity(ChatMessageDao dao, List<String> imageUrls) {
    var entity = toEntity(dao);
    entity.setImageIds(imageUrls); // Replace IDs with URLs for response
    return entity;
  }

  public static ChatMessageDao toDao(ChatMessage entity) {
    var dao = new ChatMessageDao();
    dao.setId(entity.getId());
    dao.setSwapRequestId(entity.getSwapRequestId());
    dao.setSender(UserMapper.toDao(entity.getSender()));
    dao.setMessage(entity.getMessage());
    dao.setImageIds(entity.getImageIds()); // Map image IDs
    var currentTime = Instant.now();
    dao.setSentAt(entity.getSentAt() == null ? currentTime : entity.getSentAt());
    dao.setReadByReceiver(entity.isReadByReceiver());
    return dao;
  }
}
