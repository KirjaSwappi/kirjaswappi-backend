/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.http.controllers.CityController;
import com.kirjaswappi.backend.service.CityService;
import com.kirjaswappi.backend.service.entities.City;

@WebMvcTest(CityController.class)
@Import(CustomMockMvcConfiguration.class)
class CityApiIntegrationTest {

  private static final String API_PATH = "/api/v1/cities";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CityService cityService;

  @Test
  @DisplayName("Should return all cities")
  void shouldReturnAllCities() throws Exception {
    when(cityService.getCities()).thenReturn(List.of(new City("Helsinki"), new City("Tampere")));

    mockMvc.perform(get(API_PATH))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Helsinki"))
        .andExpect(jsonPath("$[1].name").value("Tampere"));
  }

  @Test
  @DisplayName("Should return empty list when no cities")
  void shouldReturnEmptyListWhenNoCities() throws Exception {
    when(cityService.getCities()).thenReturn(List.of());

    mockMvc.perform(get(API_PATH))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(0));
  }
}
