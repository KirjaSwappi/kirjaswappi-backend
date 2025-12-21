/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.API_BASE;
import static com.kirjaswappi.backend.common.utils.Constants.CITIES;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kirjaswappi.backend.http.dtos.responses.CityResponse;
import com.kirjaswappi.backend.service.CityService;
import com.kirjaswappi.backend.service.entities.City;

@RestController
@RequestMapping(API_BASE + CITIES)
@Validated
@Tag(name = "Cities", description = "API for managing cities in Finland")
public class CityController {
  @Autowired
  private CityService cityService;

  @GetMapping
  @Operation(summary = "Find all cities in Finland.", description = "Returns a list of major cities in Finland sorted alphabetically.", responses = {
      @ApiResponse(responseCode = "200", description = "List of cities in Finland.")
  })
  public ResponseEntity<List<CityResponse>> findCities() {
    List<City> cities = cityService.getCities();
    List<CityResponse> cityResponses = cities.stream()
        .map(city -> new CityResponse(city.getName()))
        .toList();
    return ResponseEntity.status(HttpStatus.OK).body(cityResponses);
  }
}
