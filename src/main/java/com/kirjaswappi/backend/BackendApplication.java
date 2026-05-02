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
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableAsync
@EnableScheduling
@EnableCaching
@SpringBootApplication
public class BackendApplication {

  public static void main(String[] args) {
    String activeProfile = resolveActiveProfile();

    log.info("*** KirjaSwappi Backend ***");
    log.info("Running on {} profile", activeProfile);

    // Production deploy guard. If the deploy environment marks itself as
    // production (KIRJASWAPPI_PRODUCTION=true) but the active profile is
    // not 'cloud', fail fast — LocalSecurityConfig disables all security.
    String productionFlag = System.getenv("KIRJASWAPPI_PRODUCTION");
    if ("true".equalsIgnoreCase(productionFlag)
        && !java.util.Arrays.asList(activeProfile.split(",")).contains("cloud")) {
      log.error("Refusing to start: KIRJASWAPPI_PRODUCTION=true but active profile '{}' does not include 'cloud'. "
          + "LocalSecurityConfig disables authentication; running with this combination would expose the API.",
          activeProfile);
      System.exit(78); // EX_CONFIG
    }

    SpringApplication.run(BackendApplication.class, args);

    log.info("Application started successfully");
  }

  private static String resolveActiveProfile() {
    String fromProperty = System.getProperty("spring.profiles.active");
    if (fromProperty != null && !fromProperty.isBlank()) {
      return fromProperty;
    }
    String fromEnv = System.getenv("SPRING_PROFILES_ACTIVE");
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv;
    }
    return "default";
  }
}
