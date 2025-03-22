/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

import com.kirjaswappi.backend.service.enums.Condition;
import com.kirjaswappi.backend.service.enums.Language;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Book {
  private String id;
  private String title;
  private String author;
  private String description;
  private Language language;
  private Condition condition;
  private List<Genre> genres;
  private String coverPhoto;
  private MultipartFile coverPhotoFile;
  private User owner;
  private ExchangeCondition exchangeCondition;
}
