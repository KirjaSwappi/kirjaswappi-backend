/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@Profile("cloud")
public class RedisStartupCleaner implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(RedisStartupCleaner.class);
  private final RedisConnectionFactory redisConnectionFactory;

  public RedisStartupCleaner(RedisConnectionFactory redisConnectionFactory) {
    this.redisConnectionFactory = redisConnectionFactory;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    try (var connection = redisConnectionFactory.getConnection()) {
      connection.serverCommands().flushAll();
      logger.info("Redis cache cleared successfully on startup.");
    } catch (Exception e) {
      logger.error("Failed to clear Redis cache on startup", e);
    }
  }
}
