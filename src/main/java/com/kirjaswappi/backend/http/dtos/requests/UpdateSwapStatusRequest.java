/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.dtos.requests;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import com.kirjaswappi.backend.http.validations.ValidationUtil;
import com.kirjaswappi.backend.service.enums.SwapStatus;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;

@Getter
@Setter
public class UpdateSwapStatusRequest {
  @NotBlank(message = "Status cannot be blank")
  @Schema(description = "The new status for the swap request", example = "Accepted", allowableValues = { "Pending",
      "Accepted", "Reserved", "Rejected", "Expired" })
  private String status;

  public void validate() {
    if (!ValidationUtil.validateNotBlank(this.status)) {
      throw new BadRequestException("statusCannotBeBlank");
    }

    // Validate that the status is a valid SwapStatus
    try {
      SwapStatus.fromCode(this.status);
    } catch (BadRequestException e) {
      throw new BadRequestException("invalidSwapStatus", this.status);
    }
  }
}