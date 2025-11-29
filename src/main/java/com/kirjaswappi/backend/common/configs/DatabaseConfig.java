/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

@Slf4j
@Configuration
@Profile("!test")
public class DatabaseConfig {
  @Value("${spring.data.mongodb.uri}")
  private String databaseUri;

  @Value("${spring.data.mongodb.database}")
  private String databaseName;

  @Bean
  public MongoClient mongoClient() {
    ConnectionString connectionString = new ConnectionString(databaseUri);

    log.info("=== Custom MongoDB Configuration ===");
    log.info("  - URI hosts: {}", connectionString.getHosts());
    log.info("  - Database from URI: {}", connectionString.getDatabase());
    log.info("  - Configured database name: {}", databaseName);
    log.info("  - Will use database: {}", databaseName);

    if (connectionString.getDatabase() != null && !connectionString.getDatabase().equals(databaseName)) {
      log.warn("WARNING: URI contains database '{}' but we're overriding it with '{}'",
          connectionString.getDatabase(), databaseName);
    }

    MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .build();

    return MongoClients.create(settings);
  }

  @Bean
  public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
    log.info("Creating MongoDatabaseFactory with database: {}", databaseName);
    return new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
  }

  @Bean
  public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
    log.info("Creating MongoTemplate with configured MongoDatabaseFactory");
    return new MongoTemplate(mongoDatabaseFactory);
  }

  @Bean
  public GridFSBucket gridFSBucket(MongoClient mongoClient) {
    return GridFSBuckets.create(mongoClient.getDatabase(databaseName));
  }
}
