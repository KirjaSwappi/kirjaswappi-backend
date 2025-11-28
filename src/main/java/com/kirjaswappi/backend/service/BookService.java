/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.SwappableBookDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.BookRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.mapper.*;
import com.kirjaswappi.backend.service.entities.Book;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.entities.SwappableBook;
import com.kirjaswappi.backend.service.exceptions.BookNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;
import com.kirjaswappi.backend.service.filters.FindAllBooksFilter;

@Service
@Transactional
@RequiredArgsConstructor
public class BookService {

  private final BookRepository bookRepository;

  private final UserRepository userRepository;

  private final GenreService genreService;

  private final PhotoService photoService;

  private static final List<String> ALLOWED_SORT_FIELDS = Arrays.asList("title", "author", "language", "condition",
      "genres.name", "bookUpdatedAt");

  public Book createBook(Book book) {
    setValidSwappableGenresIfExists(book);
    var bookDao = BookMapper.toDao(book);
    addGenresToBook(book, bookDao);
    setOwnerToBook(book, bookDao);
    var savedDao = bookRepository.save(bookDao);
    savedDao = addCoverPhotoToBook(book, savedDao);
    addCoverPhotoToSwappableBooksIfExists(book, savedDao);
    addBookToOwner(savedDao);
    return getBookById(savedDao.id());
  }

  // TODO: send notification to the swap requests senders for this book.
  public Book updateBook(Book updatedBook) {
    var existingBookDao = bookRepository.findByIdAndIsDeletedFalse(updatedBook.id())
        .orElseThrow(() -> new BookNotFoundException(updatedBook.id()));
    updateExistingDaoWithNewProperties(updatedBook, existingBookDao);
    var updatedBookDao = bookRepository.save(existingBookDao);
    updatedBookDao = updateBookCoverPhoto(updatedBook, updatedBookDao);
    addCoverPhotoToSwappableBooksIfExists(updatedBook, updatedBookDao);
    return getBookById(updatedBookDao.id());
  }

  public Book getBookById(String id) {
    var bookDao = bookRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new BookNotFoundException(id));
    if (bookDao.swapCondition().swappableBooks() != null) {
      var filteredList = bookDao.swapCondition().swappableBooks()
          .stream()
          .filter(sb -> !sb.isDeleted())
          .toList();
      bookDao.swapCondition().swappableBooks(filteredList);
    }
    return bookWithImageUrlAndOwner(bookDao);
  }

  public SwappableBook getSwappableBookById(String swappableBookId) {
    var bookDao = bookRepository.findByIsDeletedFalseAndSwapConditionSwappableBooksId(swappableBookId);
    var swappableBookDao = bookDao.flatMap(book -> book.swapCondition()
        .swappableBooks()
        .stream()
        .filter(sb -> sb.id().equals(swappableBookId) && !sb.isDeleted())
        .findFirst()).orElseThrow(BookNotFoundException::new);
    return swappableBookWithImageUrl(swappableBookDao);
  }

  public Page<@NonNull Book> getAllBooksByFilter(FindAllBooksFilter filter, Pageable pageable) {
    var criteria = filter.buildSearchAndFilterCriteria();
    return getBooks(pageable, criteria);
  }

  // keeping the book cover photo for future references
  public void deleteBook(String id) {
    var bookDao = bookRepository.findByIdAndIsDeletedFalse(id).orElseThrow(() -> new BookNotFoundException(id));
    removeBookFromOwner(bookDao);
    bookRepository.deleteLogically(id);
  }

  public void deleteAllBooks() {
    bookRepository.findAllByIsDeletedFalse().forEach(bookDao -> deleteBook(bookDao.id()));
  }

  private void addCoverPhotoToSwappableBooksIfExists(Book parentBook, BookDao bookDao) {
    var parentSwapBooks = getValidSwappableBooks(parentBook);
    var daoSwapBooks = getValidSwappableBooks(bookDao);

    if (parentSwapBooks.size() != daoSwapBooks.size()) {
      throw new IllegalStateException("Swappable books size doesn't match");
    }

    if (parentSwapBooks.isEmpty()) {
      return;
    }

    for (int i = 0; i < parentSwapBooks.size(); i++) {
      var coverPhoto = parentSwapBooks.get(i).getCoverPhotoFile();
      var bookDaoId = daoSwapBooks.get(i).id();
      var uniqueId = bookDaoId + "-SwappableBookCoverPhoto";

      photoService.addBookCoverPhoto(coverPhoto, uniqueId);
      daoSwapBooks.get(i).coverPhoto(uniqueId);
    }
    bookRepository.save(bookDao);
  }

  private List<SwappableBook> getValidSwappableBooks(Book book) {
    var books = book.swapCondition() != null ? book.swapCondition().swappableBooks() : null;
    return books == null ? List.of() : books.stream().filter(b -> !b.isDeleted()).toList();
  }

  private List<SwappableBookDao> getValidSwappableBooks(BookDao bookDao) {
    var books = bookDao.swapCondition() != null ? bookDao.swapCondition().swappableBooks() : null;
    return books == null ? List.of() : books.stream().filter(b -> !b.isDeleted()).toList();
  }

  private void updateExistingDaoWithNewProperties(Book updatedBook, BookDao existingBookDao) {
    existingBookDao.title(updatedBook.title());
    existingBookDao.author(updatedBook.author());
    existingBookDao.description(updatedBook.description());
    existingBookDao.language(updatedBook.language().code());
    existingBookDao.condition(updatedBook.condition().code());
    addGenresToBook(updatedBook, existingBookDao);
    setValidSwappableGenresIfExists(updatedBook);
    keepOldSwappableBooksForReferenceIfExists(updatedBook, existingBookDao);
    existingBookDao.swapCondition(SwapConditionMapper.toDao(updatedBook.swapCondition()));
    existingBookDao.location(BookLocationMapper.toDao(updatedBook.location()));
    existingBookDao.bookUpdatedAt(Instant.now());
  }

  // also, keeping the photos for reference
  private static void keepOldSwappableBooksForReferenceIfExists(Book updatedBook, BookDao existingBookDao) {
    var oldSwappableBookDaos = existingBookDao.swapCondition().swappableBooks();
    if (oldSwappableBookDaos != null && !oldSwappableBookDaos.isEmpty()) {
      oldSwappableBookDaos.forEach(swappableBookDao -> swappableBookDao.isDeleted(true));
      var oldSwappableBooks = oldSwappableBookDaos.stream().map(SwappableBookMapper::toEntity).toList();
      updatedBook.swapCondition().swappableBooks().addAll(oldSwappableBooks);
    }
  }

  private void setValidSwappableGenresIfExists(Book book) {
    var swappableGenres = book.swapCondition().swappableGenres();
    if (swappableGenres == null || swappableGenres.isEmpty()) {
      return;
    }
    List<Genre> validGenres = swappableGenres.stream()
        .map(genre -> genreService.getGenreByName(genre.getName())).toList();
    book.swapCondition().swappableGenres(validGenres);
  }

  private void addGenresToBook(Book book, BookDao dao) {
    dao.genres(book.genres().stream()
        .map(genre -> GenreMapper.toDao(genreService.getGenreByName(genre.getName())))
        .toList());
  }

  private void setOwnerToBook(Book book, BookDao bookDao) {
    var owner = userRepository.findByIdAndIsEmailVerifiedTrue(book.owner().id())
        .orElseThrow(() -> new UserNotFoundException(book.owner().id()));
    bookDao.owner(owner);
  }

  private BookDao updateBookCoverPhoto(Book book, BookDao dao) {
    deleteExistingCoverPhoto(dao);
    return addCoverPhotoToBook(book, dao);
  }

  private BookDao addCoverPhotoToBook(Book book, BookDao dao) {
    var coverPhotoIds = new ArrayList<String>();
    var index = 1;
    for (var coverPhotoFile : book.coverPhotoFiles()) {
      var uniqueId = dao.id() + "-" + "BookCoverPhoto" + "-" + index;
      photoService.addBookCoverPhoto(coverPhotoFile, uniqueId);
      coverPhotoIds.add(uniqueId);
      index++;
    }
    dao.coverPhotos(coverPhotoIds);
    return bookRepository.save(dao);
  }

  private void addBookToOwner(BookDao dao) {
    var owner = userRepository.findByIdAndIsEmailVerifiedTrue(dao.owner().id())
        .orElseThrow(() -> new UserNotFoundException(dao.owner().id()));
    owner.books(Optional.ofNullable(owner.books()).orElseGet(ArrayList::new));
    owner.books().add(dao);
    userRepository.save(owner);
  }

  private void deleteExistingCoverPhoto(BookDao dao) {
    if (dao.coverPhotos() != null) {
      for (var coverPhoto : dao.coverPhotos()) {
        photoService.deleteBookCoverPhoto(coverPhoto);
      }
    }
  }

  private void removeBookFromOwner(BookDao dao) {
    var owner = userRepository.findByIdAndIsEmailVerifiedTrue(dao.owner().id())
        .orElseThrow(() -> new UserNotFoundException(dao.owner().id()));
    if (owner.books() != null) {
      owner.books(owner.books().stream()
          .filter(book -> !book.id().equals(dao.id()))
          .toList());
      userRepository.save(owner);
    }
  }

  private Pageable getPageableWithValidSortingCriteria(Pageable pageable) {
    if (!pageable.getSort().isSorted()) {
      // if no sorting is provided, then add default sorting by offeredAgo
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
          Sort.by(Sort.Direction.DESC, "bookUpdatedAt"));
      return pageable;
    }

    List<Sort.Order> allowedOrders = pageable.getSort().stream()
        .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
        .toList();

    if (allowedOrders.isEmpty()) {
      // remove sorting if no valid sorting criteria is found
      return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(allowedOrders));
  }

  private Book bookWithCoverPhotoUrl(BookDao bookDao) {
    var book = fetchImageUrlForBookCoverPhoto(bookDao);
    fetchImageUrlForSwappableBooksIfExists(book);
    return book;
  }

  private Book bookWithImageUrlAndOwner(BookDao bookDao) {
    var book = bookWithCoverPhotoUrl(bookDao);
    return bookWithOwner(bookDao.owner(), book);
  }

  private SwappableBook swappableBookWithImageUrl(SwappableBookDao bookDao) {
    var coverPhotoImageUrl = photoService.getBookCoverPhoto(bookDao.coverPhoto());
    return SwappableBookMapper.toEntity(bookDao, coverPhotoImageUrl);
  }

  @NotNull
  private static Book bookWithOwner(UserDao userDao, Book book) {
    return BookMapper.setOwner(userDao, book);
  }

  @NotNull
  private Book fetchImageUrlForBookCoverPhoto(BookDao bookDao) {
    var coverPhotoImageUrls = new ArrayList<String>();
    for (var uniqueId : bookDao.coverPhotos()) {
      var imageUrl = photoService.getBookCoverPhoto(uniqueId);
      coverPhotoImageUrls.add(imageUrl);
    }
    return BookMapper.toEntity(bookDao, coverPhotoImageUrls);
  }

  private void fetchImageUrlForSwappableBooksIfExists(Book parentBook) {
    var swappableBooks = parentBook.swapCondition().swappableBooks();
    if (swappableBooks == null || swappableBooks.isEmpty()) {
      return;
    }
    for (int i = 0; i < swappableBooks.size(); i++) {
      var uniqueId = swappableBooks.get(i).getCoverPhoto();
      String coverPhotoUrl = photoService.getBookCoverPhoto(uniqueId);
      parentBook.swapCondition().swappableBooks().get(i).setCoverPhoto(coverPhotoUrl);
    }
  }

  public List<Book> getMoreBooksOfTheUser(String bookId) {
    var bookDao = bookRepository.findByIdAndIsDeletedFalse(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));
    var owner = bookDao.owner();
    assert owner.books() != null;
    return owner.books().stream()
        .filter(book -> !book.id().equals(bookId)) // Exclude the current book
        .map(this::bookWithImageUrlAndOwner).toList();
  }

  public Page<@NonNull Book> getUserBooksByFilter(String id, @Valid FindAllBooksFilter filter, Pageable pageable) {
    filter.setOwnerId(id);
    var criteria = filter.buildSearchAndFilterCriteria();
    return getBooks(pageable, criteria);
  }

  @NotNull
  private PageImpl<@NonNull Book> getBooks(Pageable pageable, Criteria criteria) {
    pageable = getPageableWithValidSortingCriteria(pageable);
    var bookDaos = bookRepository.findAllBooksByFilter(criteria, pageable);
    var books = bookDaos.stream().map(this::bookWithImageUrlAndOwner).toList();
    return new PageImpl<>(books, pageable, bookDaos.getTotalElements());
  }

  /**
   * Find books near a specific location within a given radius.
   *
   * @param latitude  the latitude coordinate
   * @param longitude the longitude coordinate
   * @param radiusKm  the search radius in kilometers
   * @param pageable  pagination information
   * @return page of books near the specified location
   */
  public Page<@NonNull Book> findBooksNearLocation(Double latitude, Double longitude, Integer radiusKm,
      Pageable pageable) {
    var filter = new FindAllBooksFilter();
    filter.setNearLatitude(latitude);
    filter.setNearLongitude(longitude);
    filter.setRadiusKm(radiusKm);
    return getAllBooksByFilter(filter, pageable);
  }

  /**
   * Find books in a specific city.
   *
   * @param city     the city name
   * @param pageable pagination information
   * @return page of books in the specified city
   */
  public Page<@NonNull Book> findBooksInCity(String city, Pageable pageable) {
    var filter = new FindAllBooksFilter();
    filter.setCity(city);
    return getAllBooksByFilter(filter, pageable);
  }

  /**
   * Find books in a specific country.
   *
   * @param country  the country name
   * @param pageable pagination information
   * @return page of books in the specified country
   */
  public Page<@NonNull Book> findBooksInCountry(String country, Pageable pageable) {
    var filter = new FindAllBooksFilter();
    filter.setCountry(country);
    return getAllBooksByFilter(filter, pageable);
  }
}
