/*
 * Copyright (c) 2026 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RateLimiterServiceTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOps;
  private RateLimiterService rateLimiterService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    rateLimiterService = new RateLimiterService(redisTemplate);
  }

  @Test
  @DisplayName("isRateLimited returns false when no attempts recorded")
  void isRateLimitedReturnsFalseWhenNoAttempts() {
    when(valueOps.get("k")).thenReturn(null);
    assertFalse(rateLimiterService.isRateLimited("k", 5));
  }

  @Test
  @DisplayName("isRateLimited returns true when attempts >= max")
  void isRateLimitedReturnsTrueWhenOverThreshold() {
    when(valueOps.get("k")).thenReturn("5");
    assertTrue(rateLimiterService.isRateLimited("k", 5));
  }

  @Test
  @DisplayName("isRateLimited fails OPEN when Redis throws (legacy callers)")
  void isRateLimitedFailsOpenOnRedisError() {
    when(valueOps.get("k")).thenThrow(new RedisConnectionFailureException("down"));
    assertFalse(rateLimiterService.isRateLimited("k", 5));
  }

  @Test
  @DisplayName("isRateLimitedFailClosed fails CLOSED when Redis throws")
  void isRateLimitedFailClosedFailsClosedOnRedisError() {
    when(valueOps.get("k")).thenThrow(new RedisConnectionFailureException("down"));
    assertTrue(rateLimiterService.isRateLimitedFailClosed("k", 5));
  }

  @Test
  @DisplayName("isRateLimitedFailClosed returns true at threshold")
  void isRateLimitedFailClosedHonoursThreshold() {
    when(valueOps.get("k")).thenReturn("10");
    assertTrue(rateLimiterService.isRateLimitedFailClosed("k", 10));
  }

  @Test
  @DisplayName("recordAttempt swallows Redis errors")
  void recordAttemptSwallowsErrors() {
    when(valueOps.increment("k")).thenThrow(new RedisConnectionFailureException("down"));
    rateLimiterService.recordAttempt("k", Duration.ofMinutes(15));
  }
}
