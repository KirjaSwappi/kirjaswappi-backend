/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.jpa.repositories.CityRepository;
import com.kirjaswappi.backend.mapper.CityMapper;
import com.kirjaswappi.backend.service.entities.City;

@Service
@Transactional
@RequiredArgsConstructor
public class CityService {
  private final CityRepository cityRepository;

  public List<City> getCities() {
    return cityRepository.findAll().stream()
        .map(CityMapper::toEntity)
        .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
        .toList();
  }
}
