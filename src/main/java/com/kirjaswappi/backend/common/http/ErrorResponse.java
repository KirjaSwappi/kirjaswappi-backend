/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * REST specific POJO for how errors should be represented in response to a REST
 * request.
 */
@Schema(description = "Standard error response format")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "Error details") Error error
) {
  @Schema(description = "Error information")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Error {
    @Schema(description = "Error code", example = "invalidEmailAddress")
    private final String code;

    @Schema(description = "Human-readable error message", example = "The request contains invalid data")
    private final String message;

    @Schema(description = "Target of the error (optional)", example = "username")
    private final String target;

    @Schema(description = "Detailed error information")
    private List<ErrorDetail> details;

    public Error(String code, String message) {
      this(code, message, null);
    }

    public Error(String code, String message, String target) {
      this.code = code;
      this.message = message;
      this.target = target;
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public String getTarget() {
      return target;
    }

    public List<ErrorDetail> getDetails() {
      return details;
    }

    public void addErrorDetail(String code, String message, String target) {
      if (Objects.isNull(this.details)) {
        this.details = new LinkedList<>();
      }
      details.add(new ErrorDetail(code, message, target));
    }
  }

  @Schema(description = "Detailed error information")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record ErrorDetail(
      @Schema(description = "Detail error code", example = "REQUIRED_FIELD") String code,
      @Schema(description = "Detail error message", example = "Username is required") String message,
      @Schema(description = "Detail error target", example = "username") String target
  ) {
  }
}
