/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@EnableAsync
@EnableCaching
@SpringBootApplication
public class BackendApplication {

  public static void main(String[] args) {
    String activeProfile = System.getProperty("spring.profiles.active", "default");

    log.info("*** KirjaSwappi Backend ***");
    log.info("Running on {} profile", activeProfile);

    SpringApplication.run(BackendApplication.class, args);

    log.info("Application started successfully");
  }
}
