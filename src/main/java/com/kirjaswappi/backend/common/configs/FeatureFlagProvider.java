/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import jakarta.annotation.PostConstruct;

import io.getunleash.DefaultUnleash;
import io.getunleash.FakeUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cloud")
public class FeatureFlagProvider {
  private static final Logger logger = LoggerFactory.getLogger(FeatureFlagProvider.class);

  @Value("${unleash.instanceId}")
  private String instanceId;

  @Value("${unleash.url}")
  private String apiUrl;

  @Value("${unleash.apiKey:}")
  private String apiKey;

  private Unleash unleash = new FakeUnleash();

  @PostConstruct
  private void initializeUnleash() {
    if (apiKey == null || apiKey.isBlank()) {
      logger.warn(
          "UNLEASH_API_KEY is empty; using FakeUnleash fallback and skipping remote feature flag polling");
      return;
    }

    UnleashConfig config = UnleashConfig.builder()
        .appName("Kirjaswappi-backend")
        .instanceId(instanceId)
        .unleashAPI(apiUrl)
        .apiKey(apiKey)
        .build();
    this.unleash = new DefaultUnleash(config);
    logger.info("Initialized Unleash client for {}", apiUrl);
  }

  public boolean isFeatureEnabled(String featureName) {
    return unleash.isEnabled(featureName);
  }
}
