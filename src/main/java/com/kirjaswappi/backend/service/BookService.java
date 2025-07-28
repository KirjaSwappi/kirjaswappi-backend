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

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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
public class BookService {
  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GenreService genreService;

  @Autowired
  private PhotoService photoService;

  private static final List<String> ALLOWED_SORT_FIELDS = Arrays.asList("title", "author", "language", "condition",
      "genres.name", "offeredAgo");

  public Book createBook(Book book) {
    setValidSwappableGenresIfExists(book);
    var bookDao = BookMapper.toDao(book);
    addGenresToBook(book, bookDao);
    setOwnerToBook(book, bookDao);
    var savedDao = bookRepository.save(bookDao);
    savedDao = addCoverPhotoToBook(book, savedDao);
    addCoverPhotoToSwappableBooksIfExists(book, savedDao);
    addBookToOwner(savedDao);
    return getBookById(savedDao.getId());
  }

  // TODO: send notification to the swap requests senders for this book.
  public Book updateBook(Book updatedBook) {
    var existingBookDao = bookRepository.findByIdAndIsDeletedFalse(updatedBook.getId())
        .orElseThrow(() -> new BookNotFoundException(updatedBook.getId()));
    updateExistingDaoWithNewProperties(updatedBook, existingBookDao);
    var updatedBookDao = bookRepository.save(existingBookDao);
    updatedBookDao = updateBookCoverPhoto(updatedBook, updatedBookDao);
    addCoverPhotoToSwappableBooksIfExists(updatedBook, updatedBookDao);
    return getBookById(updatedBookDao.getId());
  }

  public Book getBookById(String id) {
    var bookDao = bookRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new BookNotFoundException(id));
    if (bookDao.getSwapCondition().getSwappableBooks() != null) {
      var filteredList = bookDao.getSwapCondition().getSwappableBooks()
          .stream()
          .filter(sb -> !sb.isDeleted())
          .toList();
      bookDao.getSwapCondition().setSwappableBooks(filteredList);
    }
    return bookWithImageUrlAndOwner(bookDao);
  }

  public SwappableBook getSwappableBookById(String swappableBookId) {
    var bookDao = bookRepository.findByIsDeletedFalseAndSwapConditionSwappableBooksId(swappableBookId);
    var swappableBookDao = bookDao.flatMap(book -> book.getSwapCondition()
        .getSwappableBooks()
        .stream()
        .filter(sb -> sb.getId().equals(swappableBookId) && !sb.isDeleted())
        .findFirst()).orElseThrow(BookNotFoundException::new);
    return swappableBookWithImageUrl(swappableBookDao);
  }

  public Page<Book> getAllBooksByFilter(FindAllBooksFilter filter, Pageable pageable) {
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
    bookRepository.findAllByIsDeletedFalse().forEach(bookDao -> deleteBook(bookDao.getId()));
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
      var bookDaoId = daoSwapBooks.get(i).getId();
      var uniqueId = bookDaoId + "-SwappableBookCoverPhoto";

      photoService.addBookCoverPhoto(coverPhoto, uniqueId);
      daoSwapBooks.get(i).setCoverPhoto(uniqueId);
    }
    bookRepository.save(bookDao);
  }

  private List<SwappableBook> getValidSwappableBooks(Book book) {
    var books = book.getSwapCondition() != null ? book.getSwapCondition().getSwappableBooks() : null;
    return books == null ? List.of() : books.stream().filter(b -> !b.isDeleted()).toList();
  }

  private List<SwappableBookDao> getValidSwappableBooks(BookDao bookDao) {
    var books = bookDao.getSwapCondition() != null ? bookDao.getSwapCondition().getSwappableBooks() : null;
    return books == null ? List.of() : books.stream().filter(b -> !b.isDeleted()).toList();
  }

  private void updateExistingDaoWithNewProperties(Book updatedBook, BookDao existingBookDao) {
    existingBookDao.setTitle(updatedBook.getTitle());
    existingBookDao.setAuthor(updatedBook.getAuthor());
    existingBookDao.setDescription(updatedBook.getDescription());
    existingBookDao.setLanguage(updatedBook.getLanguage().getCode());
    existingBookDao.setCondition(updatedBook.getCondition().getCode());
    addGenresToBook(updatedBook, existingBookDao);
    setValidSwappableGenresIfExists(updatedBook);
    keepOldSwappableBooksForReferenceIfExists(updatedBook, existingBookDao);
    existingBookDao.setSwapCondition(SwapConditionMapper.toDao(updatedBook.getSwapCondition()));
    existingBookDao.setBookUpdatedAt(Instant.now());
    existingBookDao.setOfferedAgo(existingBookDao.getOfferedAgo());
  }

  // also, keeping the photos for reference
  private static void keepOldSwappableBooksForReferenceIfExists(Book updatedBook, BookDao existingBookDao) {
    var oldSwappableBookDaos = existingBookDao.getSwapCondition().getSwappableBooks();
    if (oldSwappableBookDaos != null && !oldSwappableBookDaos.isEmpty()) {
      oldSwappableBookDaos.forEach(swappableBookDao -> swappableBookDao.setDeleted(true));
      var oldSwappableBooks = oldSwappableBookDaos.stream().map(SwappableBookMapper::toEntity).toList();
      updatedBook.getSwapCondition().getSwappableBooks().addAll(oldSwappableBooks);
    }
  }

  private void setValidSwappableGenresIfExists(Book book) {
    var swappableGenres = book.getSwapCondition().getSwappableGenres();
    if (swappableGenres == null || swappableGenres.isEmpty()) {
      return;
    }
    List<Genre> validGenres = swappableGenres.stream()
        .map(genre -> genreService.getGenreByName(genre.getName())).toList();
    book.getSwapCondition().setSwappableGenres(validGenres);
  }

  private void addGenresToBook(Book book, BookDao dao) {
    dao.setGenres(book.getGenres().stream()
        .map(genre -> GenreMapper.toDao(genreService.getGenreByName(genre.getName())))
        .toList());
  }

  private void setOwnerToBook(Book book, BookDao bookDao) {
    var owner = userRepository.findByIdAndIsEmailVerifiedTrue(book.getOwner().getId())
        .orElseThrow(() -> new UserNotFoundException(book.getOwner().getId()));
    bookDao.setOwner(owner);
  }

  private BookDao updateBookCoverPhoto(Book book, BookDao dao) {
    deleteExistingCoverPhoto(dao);
    return addCoverPhotoToBook(book, dao);
  }

  private BookDao addCoverPhotoToBook(Book book, BookDao dao) {
    var coverPhotoIds = new ArrayList<String>();
    var index = 1;
    for (var coverPhotoFile : book.getCoverPhotoFiles()) {
      var uniqueId = dao.getId() + "-" + "BookCoverPhoto" + "-" + index;
      photoService.addBookCoverPhoto(coverPhotoFile, uniqueId);
      coverPhotoIds.add(uniqueId);
      index++;
    }
    dao.setCoverPhotos(coverPhotoIds);
    return bookRepository.save(dao);
  }

  private void addBookToOwner(BookDao dao) {
    var owner = userRepository.findByIdAndIsEmailVerifiedTrue(dao.getOwner().getId())
        .orElseThrow(() -> new UserNotFoundException(dao.getOwner().getId()));
    owner.setBooks(Optional.ofNullable(owner.getBooks()).orElseGet(ArrayList::new));
    owner.getBooks().add(dao);
    userRepository.save(owner);
  }

  private void deleteExistingCoverPhoto(BookDao dao) {
    if (dao.getCoverPhotos() != null) {
      for (var coverPhoto : dao.getCoverPhotos()) {
        photoService.deleteBookCoverPhoto(coverPhoto);
      }
    }
  }

  private void removeBookFromOwner(BookDao dao) {
    var owner = userRepository.findByIdAndIsEmailVerifiedTrue(dao.getOwner().getId())
        .orElseThrow(() -> new UserNotFoundException(dao.getOwner().getId()));
    if (owner.getBooks() != null) {
      owner.setBooks(owner.getBooks().stream()
          .filter(book -> !book.getId().equals(dao.getId()))
          .toList());
      userRepository.save(owner);
    }
  }

  private Pageable getPageableWithValidSortingCriteria(Pageable pageable) {
    if (!pageable.getSort().isSorted()) {
      // if no sorting is provided, then add default sorting by offeredAgo
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
          Sort.by(Sort.Direction.DESC, "offeredAgo"));
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
    return bookWithOwner(bookDao.getOwner(), book);
  }

  private SwappableBook swappableBookWithImageUrl(SwappableBookDao bookDao) {
    var coverPhotoImageUrl = photoService.getBookCoverPhoto(bookDao.getCoverPhoto());
    return SwappableBookMapper.toEntity(bookDao, coverPhotoImageUrl);
  }

  @NotNull
  private static Book bookWithOwner(UserDao userDao, Book book) {
    return BookMapper.setOwner(userDao, book);
  }

  @NotNull
  private Book fetchImageUrlForBookCoverPhoto(BookDao bookDao) {
    var coverPhotoImageUrls = new ArrayList<String>();
    for (var uniqueId : bookDao.getCoverPhotos()) {
      var imageUrl = photoService.getBookCoverPhoto(uniqueId);
      coverPhotoImageUrls.add(imageUrl);
    }
    return BookMapper.toEntity(bookDao, coverPhotoImageUrls);
  }

  private void fetchImageUrlForSwappableBooksIfExists(Book parentBook) {
    var swappableBooks = parentBook.getSwapCondition().getSwappableBooks();
    if (swappableBooks == null || swappableBooks.isEmpty()) {
      return;
    }
    for (int i = 0; i < swappableBooks.size(); i++) {
      var uniqueId = swappableBooks.get(i).getCoverPhoto();
      String coverPhotoUrl = photoService.getBookCoverPhoto(uniqueId);
      parentBook.getSwapCondition().getSwappableBooks().get(i).setCoverPhoto(coverPhotoUrl);
    }
  }

  public List<Book> getMoreBooksOfTheUser(String bookId) {
    var bookDao = bookRepository.findByIdAndIsDeletedFalse(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));
    var owner = bookDao.getOwner();
    assert owner.getBooks() != null;
    return owner.getBooks().stream()
        .filter(book -> !book.getId().equals(bookId)) // Exclude the current book
        .map(this::bookWithImageUrlAndOwner).toList();
  }

  public Page<Book> getUserBooksByFilter(String id, @Valid FindAllBooksFilter filter, Pageable pageable) {
    filter.setUserId(id);
    var criteria = filter.buildSearchAndFilterCriteria();
    return getBooks(pageable, criteria);
  }

  @NotNull
  private PageImpl<Book> getBooks(Pageable pageable, Criteria criteria) {
    pageable = getPageableWithValidSortingCriteria(pageable);
    var bookDaos = bookRepository.findAllBooksByFilter(criteria, pageable);
    var books = bookDaos.stream().map(this::bookWithImageUrlAndOwner).toList();
    return new PageImpl<>(books, pageable, bookDaos.getTotalElements());
  }
}