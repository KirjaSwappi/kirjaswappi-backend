/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.components;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
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
      clearCacheKeys(connection);
      logger.info("Redis cache keys cleared successfully on startup.");
    } catch (Exception e) {
      logger.warn("Failed to clear Redis cache on startup: {}", e.getMessage());
    }
  }

  private void clearCacheKeys(RedisConnection connection) {
    List<String> patterns = List.of(
        "users::*", "books::*", "genres::*", "nested_genres::*", "imageUrls::*", "unreadCounts::*");
    for (String pattern : patterns) {
      ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
      try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
        while (cursor.hasNext()) {
          connection.keyCommands().del(cursor.next());
        }
      } catch (Exception e) {
        logger.warn("Failed to clear cache keys for pattern {}: {}", pattern, e.getMessage());
      }
    }
  }
}
