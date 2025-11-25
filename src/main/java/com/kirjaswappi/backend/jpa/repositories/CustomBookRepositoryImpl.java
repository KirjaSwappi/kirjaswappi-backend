/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.jpa.repositories;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.kirjaswappi.backend.common.exceptions.GlobalSystemException;
import com.kirjaswappi.backend.jpa.daos.BookDao;

@Repository
public class CustomBookRepositoryImpl implements CustomBookRepository {
  private static final Logger logger = LoggerFactory.getLogger(CustomBookRepositoryImpl.class);
  private static final String COLLECTION_NAME = "books";

  private final MongoTemplate mongoTemplate;

  public CustomBookRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<BookDao> findAllBooksByFilter(Criteria criteria, Pageable pageable) {
    try {
      // Build and execute the main aggregation pipeline
      List<BookDao> bookDaos = executeDataAggregation(criteria, pageable);

      // Execute count aggregation to get total results without pagination
      long totalBooks = executeTotalCountAggregation(criteria);

      return new PageImpl<>(bookDaos, pageable, totalBooks);
    } catch (Exception e) {
      logger.error("Error occurred while fetching books: {}", e.getMessage(), e);
      throw new GlobalSystemException("Error occurred while fetching books, please try again later");
    }
  }

  /**
   * Executes the main data aggregation pipeline to fetch books with pagination
   * and sorting
   */
  private List<BookDao> executeDataAggregation(Criteria criteria, Pageable pageable) {
    List<AggregationOperation> operations = new ArrayList<>();

    // Add lookup operation to join with genres collection
    operations.add(createGenresLookupOperation());

    // Add matching criteria (includes owner._id if passed in the criteria)
    operations.add(Aggregation.match(criteria));

    // Add projection operation to select required fields
    operations.add(createProjectionOperation());

    // Add sorting if provided in pageable
    if (pageable.getSort().isSorted()) {
      operations.add(Aggregation.sort(pageable.getSort()));
    }

    // Add pagination
    operations.add(Aggregation.skip(pageable.getOffset()));
    operations.add(Aggregation.limit(pageable.getPageSize()));

    Aggregation aggregation = Aggregation.newAggregation(operations);

    return mongoTemplate.aggregate(aggregation, COLLECTION_NAME, BookDao.class).getMappedResults();
  }

  /**
   * Executes the count aggregation to determine total number of matching books
   */
  private long executeTotalCountAggregation(Criteria criteria) {
    List<AggregationOperation> countOperations = new ArrayList<>();

    // Use same lookup and match operations as the main query for consistency
    countOperations.add(createGenresLookupOperation());
    countOperations.add(Aggregation.match(criteria));
    countOperations.add(Aggregation.count().as("totalBooks"));

    Aggregation countAggregation = Aggregation.newAggregation(countOperations);

    CountResult countResult = mongoTemplate.aggregate(countAggregation, COLLECTION_NAME, CountResult.class)
        .getUniqueMappedResult();

    return (countResult != null) ? countResult.getTotalBooks() : 0;
  }

  /**
   * Creates the lookup operation to join books with genres
   */
  private LookupOperation createGenresLookupOperation() {
    return LookupOperation.newLookup()
        .from("genres")
        .localField("genres.$id")
        .foreignField("_id")
        .as("genres");
  }

  /**
   * Creates the projection operation to select fields for the result
   */
  private ProjectionOperation createProjectionOperation() {
    return Aggregation.project()
        .and("_id").as("id")
        .and("title").as("title")
        .and("author").as("author")
        .and("genres").as("genres")
        .and("language").as("language")
        .and("description").as("description")
        .and("condition").as("condition")
        .and("coverPhotos").as("coverPhotos")
        .and("location").as("location")
        .and("offeredAgo").as("offeredAgo")
        .and("bookAddedAt").as("bookAddedAt")
        .and("bookUpdatedAt").as("bookUpdatedAt")
        .and("bookDeletedAt").as("bookDeletedAt")
        .and("owner").as("owner");
  }

  // Helper class for deserializing count aggregation result
  @Setter
  @Getter
  private static class CountResult {
    private long totalBooks;
  }

  @Override
  public void deleteLogically(String id) {
    Query query = new Query(Criteria.where("_id").is(id));
    Update update = new Update().set("isDeleted", true);
    update.set("bookDeletedAt", Instant.now());
    mongoTemplate.updateFirst(query, update, BookDao.class);
  }
}
