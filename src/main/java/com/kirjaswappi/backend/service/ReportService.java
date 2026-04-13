/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.jpa.daos.ReportDao;
import com.kirjaswappi.backend.jpa.repositories.ReportRepository;
import com.kirjaswappi.backend.mapper.ReportMapper;
import com.kirjaswappi.backend.service.entities.Report;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportService {

  private final ReportRepository reportRepository;

  private final UserService userService;

  public Report createReport(String reporterUserId, String reportedUserId, String reason) {
    // validate both users exist
    userService.getUser(reporterUserId);
    userService.getUser(reportedUserId);

    ReportDao dao = ReportDao.builder()
        .reporterUserId(reporterUserId)
        .reportedUserId(reportedUserId)
        .reason(reason)
        .createdAt(Instant.now())
        .build();

    ReportDao saved = reportRepository.save(dao);
    return ReportMapper.toEntity(saved);
  }
}
