/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.responses;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.service.entities.Report;

@Getter
@Setter
public class ReportResponse {
  private String id;
  private String reportedUserId;
  private String reason;
  private Instant createdAt;

  public ReportResponse(Report entity) {
    this.id = entity.id();
    this.reportedUserId = entity.reportedUserId();
    this.reason = entity.reason();
    this.createdAt = entity.createdAt();
  }
}
