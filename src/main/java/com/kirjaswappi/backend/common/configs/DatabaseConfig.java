/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

@Configuration
public class DatabaseConfig {
  @Value("${spring.data.mongodb.uri}")
  private String databaseUri;

  @Value("${spring.data.mongodb.database}")
  private String databaseName;

  @Bean
  public MongoClient mongoClient() {
    return MongoClients.create(databaseUri);
  }

  @Bean
  public GridFSBucket gridFSBucket(MongoClient mongoClient) {
    return GridFSBuckets.create(mongoClient.getDatabase(databaseName));
  }
}
