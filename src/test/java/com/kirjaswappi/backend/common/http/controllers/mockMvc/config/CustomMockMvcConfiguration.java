/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http.controllers.mockMvc.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.kirjaswappi.backend.common.http.ErrorUtils;

@TestConfiguration
public class CustomMockMvcConfiguration {
  @MockBean
  private ErrorUtils errorUtils;

  @Profile("local")
  @Bean
  public MockMvc mockMvcLocal(WebApplicationContext webApplicationContext) {
    return MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .defaultRequest(get("/").header("Host", "localhost:8080"))
        .build();
  }

  @Profile("cloud")
  @Bean
  public MockMvc mockMvcCloud(WebApplicationContext webApplicationContext) {
    return MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .defaultRequest(get("/").header("Host", "localhost:10000"))
        .build();
  }
}
