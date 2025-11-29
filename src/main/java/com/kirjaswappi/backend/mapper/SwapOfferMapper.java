/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import com.kirjaswappi.backend.jpa.daos.SwapOfferDao;
import com.kirjaswappi.backend.service.entities.SwapOffer;

public final class SwapOfferMapper {

  private SwapOfferMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static SwapOffer toEntity(SwapOfferDao dao) {
    var entity = new SwapOffer();
    if (dao.offeredBook() != null) {
      entity.offeredBook(SwappableBookMapper.toEntity(dao.offeredBook()));
    }
    if (dao.offeredGenre() != null) {
      entity.offeredGenre(GenreMapper.toEntity(dao.offeredGenre()));
    }
    return entity;
  }

  public static SwapOfferDao toDao(SwapOffer entity) {
    var dao = new SwapOfferDao();
    if (entity.offeredBook() != null) {
      dao.offeredBook(SwappableBookMapper.toDao(entity.offeredBook()));
    }
    if (entity.offeredGenre() != null) {
      dao.offeredGenre(GenreMapper.toDao(entity.offeredGenre()));
    }
    return dao;
  }
}
