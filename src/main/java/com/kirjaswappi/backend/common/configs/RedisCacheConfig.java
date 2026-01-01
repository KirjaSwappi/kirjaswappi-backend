/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kirjaswappi.backend.http.dtos.responses.NestedGenresResponse;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.User;

@Configuration
@Profile("cloud")
public class RedisCacheConfig {

  private static final String CACHE_PREFIX = "v4:";

  @Bean
  @SuppressWarnings("deprecation")
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // Support fluent accessors (like name() instead of getName()) and handle empty
    // beans
    objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    objectMapper.setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
        com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);

    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofDays(7))
        .prefixCacheNameWith(CACHE_PREFIX)
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .withCacheConfiguration("unreadCounts", config.entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, Long.class))))
        .withCacheConfiguration("users", config.entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, User.class))))
        .withCacheConfiguration("books", config.entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, Book.class))))
        .withCacheConfiguration("genres", config.entryTtl(Duration.ofDays(7))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Genre.class)))))
        .withCacheConfiguration("nested_genres", config.entryTtl(Duration.ofDays(7))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, NestedGenresResponse.class))))
        .withCacheConfiguration("imageUrls", config.entryTtl(Duration.ofDays(7))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, String.class))))
        .build();
  }
}
