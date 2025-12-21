/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.testcontainers.DockerClientFactory;

/**
 * Condition that checks if Docker is available for Testcontainers. This allows
 * tests to run even when Docker is not available.
 */
public class DockerAvailableCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    try {
      DockerClientFactory.instance().client();
      return true;
    } catch (Exception e) {
      // Log the exception to understand why Docker is not available
      System.err.println("Docker not available for Testcontainers: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
}
