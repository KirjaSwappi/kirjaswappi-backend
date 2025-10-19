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
   * This includes proper 2dsphere geospatial indexes for accurate location-based
   * book searches.
   */
  @PostConstruct
  public void createIndexes() {
    try {
      // Check if MongoDB is available before creating indexes
      mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
      // Create 2dsphere index for accurate geospatial queries
      // This requires the location data to be stored as GeoJSON Point format
      // We'll create this index manually using MongoDB commands for now
      mongoTemplate.execute(BookDao.class, collection -> {
        try {
          // Create 2dsphere index on location coordinates for accurate distance
          // calculations
          collection.createIndex(
              new org.bson.Document("location.coordinates", "2dsphere"),
              new com.mongodb.client.model.IndexOptions().name("book_location_2dsphere"));
          logger.info("Created 2dsphere index for accurate geospatial queries");
        } catch (Exception e) {
          // Index might already exist, log but don't fail
          logger.debug("2dsphere index creation skipped (may already exist): {}", e.getMessage());
        }
        return null;
      });

      // Create compound index for latitude and longitude as fallback for simple
      // queries
      Index latLngIndex = new Index()
          .on("location.latitude", org.springframework.data.domain.Sort.Direction.ASC)
          .on("location.longitude", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_lat_lng");

      mongoTemplate.indexOps(BookDao.class).createIndex(latLngIndex);
      logger.info("Created compound index for book location coordinates");

      // Create text indexes for city and country for faster text searches
      Index cityIndex = new Index().on("location.city", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_city");
      mongoTemplate.indexOps(BookDao.class).createIndex(cityIndex);

      Index countryIndex = new Index().on("location.country", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_country");
      mongoTemplate.indexOps(BookDao.class).createIndex(countryIndex);

      logger.info("Created text indexes for city and country searches");

    } catch (Exception e) {
      logger.warn("MongoDB not available or failed to create indexes: {}", e.getMessage());
      // Don't fail application startup if MongoDB is not available
    }
  }
}
