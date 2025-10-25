/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import java.util.List;

import lombok.NoArgsConstructor;

import org.springframework.stereotype.Component;

import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;

@Component
@NoArgsConstructor
public class BookMapper {
  public static Book toEntity(BookDao dao) {
    var entity = new Book();
    entity.setId(dao.getId());
    entity.setTitle(dao.getTitle());
    entity.setAuthor(dao.getAuthor());
    entity.setDescription(dao.getDescription());
    entity.setLanguage(Language.fromCode(dao.getLanguage()));
    entity.setCondition(Condition.fromCode(dao.getCondition()));
    entity
        .setGenres(dao.getGenres() == null ? List.of() : dao.getGenres().stream().map(GenreMapper::toEntity).toList());
    entity.setCoverPhotos(dao.getCoverPhotos() == null ? List.of() : dao.getCoverPhotos());
    entity.setBookAddedAt(dao.getBookAddedAt());
    entity.setBookUpdatedAt(dao.getBookUpdatedAt());
    entity.setBookDeletedAt(dao.getBookDeletedAt());
    entity.setSwapCondition(SwapConditionMapper.toEntity(dao.getSwapCondition()));
    entity.setLocation(BookLocationMapper.toEntity(dao.getLocation()));
    return entity;
  }

  public static Book toEntity(BookDao dao, List<String> imageUrls) {
    var entity = toEntity(dao);
    entity.setCoverPhotos(imageUrls == null ? List.of() : imageUrls);
    return entity;
  }

  public static Book setOwner(UserDao owner, Book book) {
    book.setOwner(UserMapper.toEntity(owner));
    return book;
  }

  public static BookDao toDao(Book entity) {
    var dao = new BookDao();
    dao.setId(entity.getId());
    dao.setTitle(entity.getTitle());
    dao.setAuthor(entity.getAuthor());
    dao.setDescription(entity.getDescription());
    dao.setLanguage(entity.getLanguage() != null ? entity.getLanguage().getCode() : null);
    dao.setCondition(entity.getCondition() != null ? entity.getCondition().getCode() : null);
    dao.setSwapCondition(
        entity.getSwapCondition() != null ? SwapConditionMapper.toDao(entity.getSwapCondition()) : null);
    dao.setGenres(
        entity.getGenres() == null ? List.of() : entity.getGenres().stream().map(GenreMapper::toDao).toList());
    dao.setCoverPhotos(entity.getCoverPhotos() == null ? List.of() : entity.getCoverPhotos());
    dao.setOwner(entity.getOwner() == null ? null : UserMapper.toDao(entity.getOwner()));
    dao.setBookAddedAt(entity.getBookAddedAt());
    dao.setBookUpdatedAt(entity.getBookUpdatedAt());
    dao.setBookDeletedAt(entity.getBookDeletedAt());
    dao.setLocation(BookLocationMapper.toDao(entity.getLocation()));
    return dao;
  }
}
