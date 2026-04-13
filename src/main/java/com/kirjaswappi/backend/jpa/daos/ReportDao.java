/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.Accessors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reports")
@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class ReportDao {
  @Id
  private String id;

  @NotNull
  private String reporterUserId;

  @NotNull
  private String reportedUserId;

  @NotNull
  private String reason;

  @NotNull
  private Instant createdAt;
}
