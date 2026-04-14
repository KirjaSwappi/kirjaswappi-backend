/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.kirjaswappi.backend.jpa.daos.UserDao;

public interface UserRepository extends MongoRepository<UserDao, String> {
  Optional<UserDao> findByEmailAndPassword(String email, String password);

  Optional<UserDao> findByEmail(String email);

  Optional<UserDao> findByEmailAndIsEmailVerified(String email, boolean isEmailVerified);

  Optional<UserDao> findByIdAndIsEmailVerifiedTrue(String id);

  List<UserDao> findAllByIsEmailVerifiedTrue();

  @Query(value = "{ '$or': [ { 'favGenres.$id': ObjectId(?0) }, { 'books.genres.$id': ObjectId(?0) } ] }", exists = true)
  boolean existsByGenreId(String genreId);
}
