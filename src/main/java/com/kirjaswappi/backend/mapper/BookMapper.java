/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import static com.kirjaswappi.backend.common.utils.ListUtil.emptyIfNull;
import static com.kirjaswappi.backend.common.utils.Util.mapIfNotNull;

import java.util.List;

import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;

public final class BookMapper {

  private BookMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static Book toEntity(BookDao dao) {
    return toEntity(dao, dao.coverPhotos());
  }

  public static Book toEntity(BookDao dao, List<String> imageUrls) {
    return Book.builder()
        .id(dao.id())
        .title(dao.title())
        .author(dao.author())
        .description(dao.description())
        .language(Language.fromCode(dao.language()))
        .condition(Condition.fromCode(dao.condition()))
        .genres(emptyIfNull(dao.genres()).stream()
            .map(GenreMapper::toEntity)
            .toList())
        .coverPhotos(emptyIfNull(imageUrls))
        .bookAddedAt(dao.bookAddedAt())
        .bookUpdatedAt(dao.bookUpdatedAt())
        .bookDeletedAt(dao.bookDeletedAt())
        .swapCondition(SwapConditionMapper.toEntity(dao.swapCondition()))
        .location(BookLocationMapper.toEntity(dao.location()))
        .build();
  }

  public static Book setOwner(UserDao owner, Book book) {
    return book.withOwner(UserMapper.toEntity(owner));
  }

  public static BookDao toDao(Book entity) {
    return BookDao.builder()
        .id(entity.id())
        .title(entity.title())
        .author(entity.author())
        .description(entity.description())
        .language(mapIfNotNull(entity.language(), Language::code))
        .condition(mapIfNotNull(entity.condition(), Condition::code))
        .swapCondition(mapIfNotNull(entity.swapCondition(), SwapConditionMapper::toDao))
        .genres(emptyIfNull(entity.genres())
            .stream()
            .map(GenreMapper::toDao)
            .toList())
        .coverPhotos(emptyIfNull(entity.coverPhotos()))
        .owner(mapIfNotNull(entity.owner(), UserMapper::toDao))
        .bookAddedAt(entity.bookAddedAt())
        .bookUpdatedAt(entity.bookUpdatedAt())
        .bookDeletedAt(entity.bookDeletedAt())
        .location(BookLocationMapper.toDao(entity.location()))
        .build();
  }
}
