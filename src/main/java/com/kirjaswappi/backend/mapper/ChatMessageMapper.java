/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import static com.kirjaswappi.backend.common.utils.Util.defaultIfNull;

import java.time.Instant;
import java.util.List;

import com.kirjaswappi.backend.jpa.daos.ChatMessageDao;
import com.kirjaswappi.backend.service.entities.ChatMessage;

public final class ChatMessageMapper {

  private ChatMessageMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static ChatMessage toEntity(ChatMessageDao dao) {
    return ChatMessage.builder()
        .id(dao.id())
        .swapRequestId(dao.swapRequestId())
        .sender(UserMapper.toEntity(dao.sender()))
        .message(dao.message())
        .imageIds(dao.imageIds()) // Map image IDs
        .sentAt(dao.sentAt())
        .readByReceiver(dao.readByReceiver())
        .build();
  }

  public static ChatMessage toEntity(ChatMessageDao dao, List<String> imageUrls) {
    return toEntity(dao)
        .withImageIds(imageUrls); // Replace IDs with URLs for response
  }

  public static ChatMessageDao toDao(ChatMessage entity) {
    return ChatMessageDao.builder()
        .id(entity.id())
        .swapRequestId(entity.swapRequestId())
        .sender(UserMapper.toDao(entity.sender()))
        .message(entity.message())
        .imageIds(entity.imageIds()) // Map image IDs
        .sentAt(defaultIfNull(entity.sentAt(), Instant.now()))
        .readByReceiver(entity.readByReceiver())
        .build();
  }
}
