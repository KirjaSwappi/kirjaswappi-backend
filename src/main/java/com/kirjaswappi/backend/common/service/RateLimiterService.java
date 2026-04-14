/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

  private final StringRedisTemplate redisTemplate;

  public boolean isRateLimited(String key, int maxAttempts) {
    try {
      String value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return false;
      }
      return Integer.parseInt(value) >= maxAttempts;
    } catch (Exception e) {
      log.warn("Redis unavailable for rate limit check, allowing request: {}", e.getMessage());
      return false;
    }
  }

  public void recordAttempt(String key, Duration window) {
    try {
      Long count = redisTemplate.opsForValue().increment(key);
      if (count != null && count == 1) {
        redisTemplate.expire(key, window);
      }
    } catch (Exception e) {
      log.warn("Redis unavailable for recording rate limit attempt: {}", e.getMessage());
    }
  }

  public void clearAttempts(String key) {
    try {
      redisTemplate.delete(key);
    } catch (Exception e) {
      log.warn("Redis unavailable for clearing rate limit attempts: {}", e.getMessage());
    }
  }
}
