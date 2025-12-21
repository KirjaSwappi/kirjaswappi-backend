/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.daos;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.Accessors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "cities")
@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class CityDao {
  @Id
  private String id;

  @NotNull
  private String name;
}
