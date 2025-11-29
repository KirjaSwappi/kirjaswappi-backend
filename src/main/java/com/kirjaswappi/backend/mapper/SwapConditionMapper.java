/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import static com.kirjaswappi.backend.common.utils.ListUtil.emptyIfNull;

import com.kirjaswappi.backend.jpa.daos.SwapConditionDao;
import com.kirjaswappi.backend.service.entities.SwapCondition;
import com.kirjaswappi.backend.service.enums.SwapType;

public final class SwapConditionMapper {

  private SwapConditionMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static SwapCondition toEntity(SwapConditionDao dao) {
    if (dao == null) {
      return new SwapCondition();
    }
    return SwapCondition.builder()
        .swapType(SwapType.fromCode(dao.swapType()))
        .giveAway(dao.giveAway())
        .openForOffers(dao.openForOffers())
        .swappableGenres(emptyIfNull(dao.swappableGenres()).stream().map(GenreMapper::toEntity).toList())
        .swappableBooks(emptyIfNull(dao.swappableBooks()).stream().map(SwappableBookMapper::toEntity).toList())
        .build();
  }

  public static SwapConditionDao toDao(SwapCondition entity) {
    return SwapConditionDao.builder()
        .swapType(entity.swapType().getCode())
        .giveAway(entity.giveAway())
        .openForOffers(entity.openForOffers())
        .swappableGenres(emptyIfNull(entity.swappableGenres()).stream().map(GenreMapper::toDao).toList())
        .swappableBooks(emptyIfNull(entity.swappableBooks()).stream().map(SwappableBookMapper::toDao).toList())
        .build();
  }
}
