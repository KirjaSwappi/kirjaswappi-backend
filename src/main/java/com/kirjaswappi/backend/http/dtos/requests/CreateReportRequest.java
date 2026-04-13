/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class CreateReportRequest {
  @Schema(description = "The ID of the user being reported.", example = "123456")
  private String reportedUserId;

  @Schema(description = "The reason for reporting the user.", example = "Inappropriate behavior")
  private String reason;

  public void validate() {
    if (!ValidationUtil.validateNotBlank(this.reportedUserId)) {
      throw new BadRequestException("reportedUserIdCannotBeBlank");
    }
    if (!ValidationUtil.validateNotBlank(this.reason)) {
      throw new BadRequestException("reasonCannotBeBlank");
    }
  }
}
