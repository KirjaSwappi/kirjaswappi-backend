/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.google.common.cache.CacheBuilder;

@Configuration
@Profile("!cloud")
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("imageUrls", "unreadCounts", "users", "books", "genres", "nested_genres") {
      @NotNull
      @Override
      protected Cache createConcurrentMapCache(@NotNull final String name) {
        long duration = 7;
        TimeUnit unit = TimeUnit.DAYS;

        if (name.equals("unreadCounts")) {
          duration = 5;
          unit = TimeUnit.MINUTES;
        } else if (name.equals("users") || name.equals("books")) {
          duration = 30;
          unit = TimeUnit.MINUTES;
        }

        return new ConcurrentMapCache(name, CacheBuilder.newBuilder()
            .expireAfterWrite(duration, unit)
            .build().asMap(), false);
      }
    };
  }
}
