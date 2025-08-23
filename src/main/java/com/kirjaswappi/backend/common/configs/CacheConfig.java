/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.cache.CacheBuilder;

@Configuration
@EnableCaching
public class CacheConfig {
  @Bean
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("imageUrls", "unreadCounts") {
      @NotNull
      @Override
      protected Cache createConcurrentMapCache(@NotNull final String name) {
        return new ConcurrentMapCache(name, CacheBuilder.newBuilder()
            .expireAfterWrite(name.equals("unreadCounts") ? 5 : 7,
                name.equals("unreadCounts") ? TimeUnit.MINUTES : TimeUnit.DAYS)
            .build().asMap(), false);
      }
    };
  }
}
