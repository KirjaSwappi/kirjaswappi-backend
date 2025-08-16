/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import java.util.List;

import lombok.NoArgsConstructor;

import org.springframework.stereotype.Component;

import com.kirjaswappi.backend.jpa.daos.SwapConditionDao;
import com.kirjaswappi.backend.service.entities.SwapCondition;
import com.kirjaswappi.backend.service.enums.SwapType;

@Component
@NoArgsConstructor
public class SwapConditionMapper {
  public static SwapCondition toEntity(SwapConditionDao dao) {
    if (dao == null) {
      return new SwapCondition();
    }
    var entity = new SwapCondition();
    entity.setSwapType(SwapType.fromCode(dao.getSwapType()));
    entity.setGiveAway(dao.isGiveAway());
    entity.setOpenForOffers(dao.isOpenForOffers());
    if (dao.getSwappableGenres() != null) {
      entity.setSwappableGenres(dao.getSwappableGenres().stream().map(GenreMapper::toEntity).toList());
    }
    if (dao.getSwappableBooks() != null) {
      entity.setSwappableBooks(dao.getSwappableBooks().stream().map(SwappableBookMapper::toEntity).toList());
    }
    return entity;
  }

  public static SwapConditionDao toDao(SwapCondition entity) {
    var dao = new SwapConditionDao();
    dao.setSwapType(entity.getSwapType().getCode());
    dao.setGiveAway(entity.isGiveAway());
    dao.setOpenForOffers(entity.isOpenForOffers());
    dao.setSwappableGenres(
        entity.getSwappableGenres() != null ? entity.getSwappableGenres().stream().map(GenreMapper::toDao).toList()
            : List.of());
    dao.setSwappableBooks(entity.getSwappableBooks() != null
        ? entity.getSwappableBooks().stream().map(SwappableBookMapper::toDao).toList()
        : List.of());
    return dao;
  }
}
