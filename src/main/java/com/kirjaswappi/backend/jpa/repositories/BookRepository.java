/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.kirjaswappi.backend.jpa.daos.BookDao;

public interface BookRepository extends MongoRepository<BookDao, String>, CustomBookRepository {
  Optional<BookDao> findByIdAndIsDeletedFalse(String id);

  List<BookDao> findAllByIsDeletedFalse();

  Optional<BookDao> findByIsDeletedFalseAndSwapConditionSwappableBooksId(String swappableBookId);

  boolean existsByGenresId(String genreId);
}
