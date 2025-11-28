/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import static com.kirjaswappi.backend.common.utils.ListUtil.emptyIfNull;

import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.service.entities.User;

public final class UserMapper {

  private UserMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static User toEntity(UserDao dao) {
    return User.builder()
        .id(dao.id())
        .firstName(dao.firstName())
        .lastName(dao.lastName())
        .email(dao.email())
        .password(dao.password())
        .streetName(dao.streetName())
        .houseNumber(dao.houseNumber())
        .zipCode(dao.zipCode())
        .city(dao.city())
        .country(dao.country())
        .phoneNumber(dao.phoneNumber())
        .aboutMe(dao.aboutMe())
        .favGenres(emptyIfNull(dao.favGenres()).stream().map(GenreMapper::toEntity).toList())
        .profilePhoto(dao.profilePhoto())
        .coverPhoto(dao.coverPhoto())
        .books(emptyIfNull(dao.books()).stream().map(BookMapper::toEntity).toList())
        .favBooks(emptyIfNull(dao.favBooks()).stream().map(BookMapper::toEntity).toList())
        .build();
  }

  // Used only to create a new user
  public static UserDao toDao(User entity, String salt) {
    return UserDao.builder()
        .firstName(entity.firstName())
        .lastName(entity.lastName())
        .email(entity.email())
        .password(entity.password())
        .salt(salt)
        .build();
  }

  public static UserDao toDao(User entity) {
    return UserDao.builder()
        .id(entity.id())
        .firstName(entity.firstName())
        .lastName(entity.lastName())
        .email(entity.email())
        .password(entity.password())
        .streetName(entity.streetName())
        .houseNumber(entity.houseNumber())
        .zipCode(entity.zipCode())
        .city(entity.city())
        .country(entity.country())
        .phoneNumber(entity.phoneNumber())
        .aboutMe(entity.aboutMe())
        .favGenres(emptyIfNull(entity.favGenres()).stream().map(GenreMapper::toDao).toList())
        .profilePhoto(entity.profilePhoto())
        .coverPhoto(entity.coverPhoto())
        .books(emptyIfNull(entity.books()).stream().map(BookMapper::toDao).toList())
        .favBooks(emptyIfNull(entity.favBooks()).stream().map(BookMapper::toDao).toList())
        .build();
  }
}
