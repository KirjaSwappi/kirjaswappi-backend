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
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import com.kirjaswappi.backend.jpa.daos.BookDao;

/**
 * Configuration class for MongoDB indexes, including geospatial indexes for
 * location-based queries. Handles both production MongoDB and embedded MongoDB
 * for testing environments.
 */
@Configuration
public class MongoIndexConfig {

  private static final Logger logger = LoggerFactory.getLogger(MongoIndexConfig.class);

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Environment environment;

  /**
   * Creates necessary indexes for the application after the bean is constructed.
   * This includes proper 2dsphere geospatial indexes for accurate location-based
   * book searches. Only runs for test and cloud profiles to avoid MongoDB
   * connection issues in CI/local environments without MongoDB.
   */
  @PostConstruct
  public void createIndexes() {
    if (shouldCreateIndexes()) {
      if (isTestProfile()) {
        logger.info("Test profile detected, creating indexes with embedded MongoDB compatibility");
        createIndexesWithDelay();
      } else if (isCloudProfile()) {
        logger.info("Cloud profile detected, creating indexes normally");
        createIndexesInternal();
      }
    } else {
      logger.info("Skipping index creation for current profile: {}",
          String.join(", ", environment.getActiveProfiles()));
    }
  }

  /**
   * Creates indexes with a small delay for embedded MongoDB environments.
   * Embedded MongoDB may need time to fully initialize.
   */
  private void createIndexesWithDelay() {
    final int maxAttempts = 5;
    long delay = 500; // initial delay in ms
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        createIndexesInternal();
        return; // Success
      } catch (Exception e) {
        if (attempt == maxAttempts) {
          logger.warn("Failed to create indexes after {} attempts: {}", maxAttempts, e.getMessage());
        } else {
          logger.info("Attempt {} to create indexes failed: {}. Retrying in {} ms...", attempt, e.getMessage(), delay);
          try {
            Thread.sleep(delay);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Index creation retry was interrupted: {}", ie.getMessage());
            return;
          }
          delay *= 2; // Exponential backoff
        }
      }
    }
  }

  /**
   * Internal method to create all necessary MongoDB indexes.
   */
  private void createIndexesInternal() {
    try {
      // Check if MongoDB is available before creating indexes
      mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
      logger.debug("MongoDB connection verified successfully");

      // Create 2dsphere index for accurate geospatial queries
      createGeospatialIndexes();

      // Create compound index for latitude and longitude as fallback
      createLocationIndexes();

      // Create text indexes for city and country for faster text searches
      createTextIndexes();

      logger.info("Successfully created all MongoDB indexes");

    } catch (Exception e) {
      boolean isTestProfile = isTestProfile();
      if (isTestProfile) {
        logger.warn("Failed to create indexes in test environment (embedded MongoDB may not be ready): {}",
            e.getMessage());
        // In test environment, don't fail the application if indexes can't be created
        // immediately
        // The embedded MongoDB might still be initializing
      } else {
        logger.error("Failed to create indexes in production environment: {}", e.getMessage());
        // In production, this is more critical, but still don't fail application
        // startup
      }
    }
  }

  /**
   * Creates geospatial indexes for location-based queries.
   */
  private void createGeospatialIndexes() {
    try {
      mongoTemplate.execute(BookDao.class, collection -> {
        try {
          // Create 2dsphere index on location coordinates for accurate distance
          // calculations
          collection.createIndex(
              new org.bson.Document("location.coordinates", "2dsphere"),
              new com.mongodb.client.model.IndexOptions().name("book_location_2dsphere"));
          logger.debug("Created 2dsphere index for accurate geospatial queries");
        } catch (Exception e) {
          // Index might already exist, log but don't fail
          logger.debug("2dsphere index creation skipped (may already exist): {}", e.getMessage());
        }
        return null;
      });
    } catch (Exception e) {
      logger.warn("Failed to create geospatial indexes: {}", e.getMessage());
    }
  }

  /**
   * Creates compound indexes for latitude and longitude coordinates.
   */
  private void createLocationIndexes() {
    try {
      Index latLngIndex = new Index()
          .on("location.latitude", org.springframework.data.domain.Sort.Direction.ASC)
          .on("location.longitude", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_lat_lng");

      mongoTemplate.indexOps(BookDao.class).createIndex(latLngIndex);
      logger.debug("Created compound index for book location coordinates");
    } catch (Exception e) {
      logger.warn("Failed to create location coordinate indexes: {}", e.getMessage());
    }
  }

  /**
   * Creates text indexes for city and country searches.
   */
  private void createTextIndexes() {
    try {
      Index cityIndex = new Index()
          .on("location.city", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_city");
      mongoTemplate.indexOps(BookDao.class).createIndex(cityIndex);

      Index countryIndex = new Index()
          .on("location.country", org.springframework.data.domain.Sort.Direction.ASC)
          .named("book_location_country");
      mongoTemplate.indexOps(BookDao.class).createIndex(countryIndex);

      logger.debug("Created text indexes for city and country searches");
    } catch (Exception e) {
      logger.warn("Failed to create text indexes: {}", e.getMessage());
    }
  }

  /**
   * Checks if indexes should be created based on active profiles.
   * Only creates indexes for test and cloud profiles.
   *
   * @return true if indexes should be created, false otherwise
   */
  private boolean shouldCreateIndexes() {
    return isTestProfile() || isCloudProfile();
  }

  /**
   * Checks if the current active profile is 'test'.
   *
   * @return true if test profile is active, false otherwise
   */
  private boolean isTestProfile() {
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      if ("test".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the current active profile is 'cloud'.
   *
   * @return true if cloud profile is active, false otherwise
   */
  private boolean isCloudProfile() {
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      if ("cloud".equals(profile)) {
        return true;
      }
    }
    return false;
  }
}
