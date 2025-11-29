/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers.mockMvc.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.ErrorUtils;

@TestConfiguration
@org.springframework.context.annotation.Import(ErrorUtils.class)
public class CustomMockMvcConfiguration {

  @Profile("test")
  @Bean
  public MockMvc mockMvcLocal(WebApplicationContext webApplicationContext) {
    return MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .defaultRequest(get("/").header("Host", "localhost:8080"))
        .build();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  @Bean
  public org.springframework.cache.CacheManager cacheManager() {
    return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
  }
}
