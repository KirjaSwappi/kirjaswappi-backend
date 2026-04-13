/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.mapper;

import com.kirjaswappi.backend.jpa.daos.ReportDao;
import com.kirjaswappi.backend.service.entities.Report;

public final class ReportMapper {

  private ReportMapper() {
    throw new IllegalStateException("Mapper class should not be instantiated");
  }

  public static Report toEntity(ReportDao dao) {
    return Report.builder()
        .id(dao.id())
        .reporterUserId(dao.reporterUserId())
        .reportedUserId(dao.reportedUserId())
        .reason(dao.reason())
        .createdAt(dao.createdAt())
        .build();
  }

  public static ReportDao toDao(Report entity) {
    return ReportDao.builder()
        .id(entity.id())
        .reporterUserId(entity.reporterUserId())
        .reportedUserId(entity.reportedUserId())
        .reason(entity.reason())
        .createdAt(entity.createdAt())
        .build();
  }
}
