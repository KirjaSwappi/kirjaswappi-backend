/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import com.kirjaswappi.backend.jpa.daos.BookDao;

/**
 * Configuration class for MongoDB indexes, including geospatial indexes for
 * location-based queries.
 */
@Configuration
public class MongoIndexConfig {

  private static final Logger logger = LoggerFactory.getLogger(MongoIndexConfig.class);

  @Autowired
  private MongoTemplate mongoTemplate;

  /**
   * Creates necessary indexes for the application after the bean is constructed.
   * This includes geospatial indexes for location-based book searches.
   */
  @PostConstruct
  public void createIndexes() {
    try {
      // Create compound index for latitude and longitude for location-based queries
      Index latLngIndex = new Index()
          .on("location.latitude", org.springframework.data.domain.Sort.Direction.ASC)
          .on("location.longitude", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_lat_lng");

      mongoTemplate.indexOps(BookDao.class).ensureIndex(latLngIndex);
      logger.info("Created compound index for book location coordinates");

      // Create text index for city and country for faster text searches
      Index cityIndex = new Index().on("location.city", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_city");
      mongoTemplate.indexOps(BookDao.class).ensureIndex(cityIndex);

      Index countryIndex = new Index().on("location.country", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_country");
      mongoTemplate.indexOps(BookDao.class).ensureIndex(countryIndex);

      logger.info("Created text indexes for city and country searches");

    } catch (Exception e) {
      logger.error("Failed to create MongoDB indexes: {}", e.getMessage(), e);
    }
  }
}
