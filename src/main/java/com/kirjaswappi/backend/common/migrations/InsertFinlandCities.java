/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.migrations;

import java.util.Arrays;
import java.util.List;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.kirjaswappi.backend.jpa.daos.CityDao;

@ChangeUnit(id = "insertFinlandCities", order = "0004", author = "mahiuddinalkamal")
public class InsertFinlandCities {

  private final MongoTemplate mongoTemplate;

  public InsertFinlandCities(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Execution
  public void executeMigration() {
    // List of major cities in Finland
    List<String> finlandCities = Arrays.asList(
        "Helsinki",
        "Espoo",
        "Tampere",
        "Vantaa",
        "Oulu",
        "Turku",
        "Jyväskylä",
        "Lahti",
        "Kuopio",
        "Kouvola",
        "Pori",
        "Joensuu",
        "Lappeenranta",
        "Hämeenlinna",
        "Vaasa",
        "Seinäjoki",
        "Rovaniemi",
        "Mikkeli",
        "Kotka",
        "Salo",
        "Porvoo",
        "Lohja",
        "Hyvinkää",
        "Järvenpää",
        "Nurmijärvi",
        "Kirkkonummi",
        "Tuusula",
        "Kerava",
        "Sipoo",
        "Siuntio");

    // Check if cities collection is empty
    if (mongoTemplate.getCollection("cities").countDocuments() == 0) {
      List<CityDao> cities = finlandCities.stream()
          .map(cityName -> CityDao.builder().name(cityName).build())
          .toList();

      mongoTemplate.insertAll(cities);
    }
  }

  @RollbackExecution
  public void rollback() {
    // Remove all cities
    mongoTemplate.dropCollection("cities");
  }
}
