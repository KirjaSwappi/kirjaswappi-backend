/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers.mockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.CityController;
import com.kirjaswappi.backend.service.CityService;
import com.kirjaswappi.backend.service.entities.City;

@WebMvcTest(CityController.class)
@Import(CustomMockMvcConfiguration.class)
class CityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CityService cityService;

  @Test
  @DisplayName("Should return all cities in Finland")
  void shouldReturnCities() throws Exception {
    List<City> cities = Arrays.asList(
        City.builder().id("1").name("Helsinki").build(),
        City.builder().id("2").name("Tampere").build(),
        City.builder().id("3").name("Turku").build());

    when(cityService.getCities()).thenReturn(cities);

    mockMvc.perform(get("/api/v1/cities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("Helsinki"))
        .andExpect(jsonPath("$[1].name").value("Tampere"))
        .andExpect(jsonPath("$[2].name").value("Turku"));
  }

  @Test
  @DisplayName("Should return empty list when no cities")
  void shouldReturnEmptyList() throws Exception {
    List<City> emptyCities = List.of();

    when(cityService.getCities()).thenReturn(emptyCities);

    mockMvc.perform(get("/api/v1/cities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }
}
