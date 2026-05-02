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

  /**
   * Returns true if the key has reached or exceeded {@code maxAttempts}. When
   * Redis is unavailable, defaults to <strong>fail-open</strong> (returns false).
   * Suitable for non-security-critical counters.
   */
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

  /**
   * Same as {@link #isRateLimited(String, int)} but <strong>fails
   * closed</strong>: when Redis is unavailable we treat the request as
   * rate-limited so brute-force protections cannot be bypassed by knocking Redis
   * offline. Use for login, password change/reset, OTP, and similar auth-adjacent
   * flows.
   */
  public boolean isRateLimitedFailClosed(String key, int maxAttempts) {
    try {
      String value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return false;
      }
      return Integer.parseInt(value) >= maxAttempts;
    } catch (Exception e) {
      log.error("Redis unavailable for auth rate limit check, denying request: {}", e.getMessage());
      return true;
    }
  }

  public void recordAttempt(String key, Duration window) {
    try {
      Long count = redisTemplate.opsForValue().increment(key);
      if (count != null && count == 1L) {
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
