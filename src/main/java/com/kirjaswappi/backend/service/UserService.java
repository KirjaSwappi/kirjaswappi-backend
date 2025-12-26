/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.common.service.exceptions.InvalidCredentials;
import com.kirjaswappi.backend.common.utils.Util;
import com.kirjaswappi.backend.jpa.daos.BookDao;
import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.BookRepository;
import com.kirjaswappi.backend.jpa.repositories.GenreRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.mapper.UserMapper;
import com.kirjaswappi.backend.service.entities.User;
import com.kirjaswappi.backend.service.exceptions.BadRequestException;
import com.kirjaswappi.backend.service.exceptions.BookNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserAlreadyExistsException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  private final GenreRepository genreRepository;

  private final PhotoService photoService;

  private final BookRepository bookRepository;

  public User addUser(User user) {
    this.checkUserExistButNotVerified(user);
    this.checkIfUserAlreadyExists(user);

    // add salt to password:
    String salt = Util.generateSalt();
    user.setPassword(user.password(), salt);

    // save user:
    UserDao dao = UserMapper.toDao(user, salt);
    dao.isEmailVerified(false);
    return UserMapper.toEntity(userRepository.save(dao));
  }

  public boolean checkIfUserExists(String email) {
    return userRepository.findByEmail(email).isPresent();
  }

  private void checkIfUserAlreadyExists(User user) {
    if (userRepository.findByEmailAndIsEmailVerified(user.email(), true).isPresent()) {
      throw new UserAlreadyExistsException(user.email());
    }
  }

  private void checkUserExistButNotVerified(User user) {
    // validate user exists and email is not verified:
    if (userRepository.findByEmailAndIsEmailVerified(user.email(), false).isPresent()) {
      throw new BadRequestException("userExistsButNotVerified", user.email());
    }
  }

  @Cacheable(value = "users", key = "#id")
  public User getUser(String id) {
    var userDao = userRepository.findByIdAndIsEmailVerifiedTrue(id)
        .orElseThrow(() -> new UserNotFoundException(id));
    setCoverPhotos(userDao);
    return UserMapper.toEntity(userDao);
  }

  private void setCoverPhotos(UserDao userDao) {
    if (userDao.books() != null) {
      userDao.books().forEach(this::fetchAndSetImage);
    }
    if (userDao.favBooks() != null) {
      userDao.favBooks().forEach(this::fetchAndSetImage);
    }
  }

  private void fetchAndSetImage(BookDao bookDao) {
    var imageUrls = new ArrayList<String>();
    if (bookDao.coverPhotos() != null) {
      bookDao.coverPhotos().forEach(uniqueId -> {
        var imageUrl = photoService.getBookCoverPhoto(uniqueId);
        imageUrls.add(imageUrl);
      });
      bookDao.coverPhotos(imageUrls);
    } else {
      bookDao.coverPhotos(List.of());
    }
  }

  public List<User> getUsers() {
    return userRepository.findAllByIsEmailVerifiedTrue().stream().map(userDao -> {
      setCoverPhotos(userDao);
      return UserMapper.toEntity(userDao);
    }).toList();
  }

  @CacheEvict(value = "users", key = "#id")
  public void deleteUser(String id) {
    // validate user exists:
    var dao = userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
    userRepository.delete(dao);
  }

  @CacheEvict(value = "users", key = "#user.id")
  public User updateUser(User user) {
    // validate user exists:
    var dao = userRepository.findByIdAndIsEmailVerifiedTrue(user.id())
        .orElseThrow(() -> new UserNotFoundException(user.id()));

    // check email verification status:
    if (!dao.isEmailVerified()) {
      throw new BadRequestException("userExistsButNotVerified", user.email());
    }

    // update user details:
    updateUserDetails(user, dao);

    return getUser(user.id());
  }

  private void updateUserDetails(User user, UserDao dao) {
    dao.firstName(user.firstName());
    dao.lastName(user.lastName());
    dao.streetName(user.streetName());
    dao.houseNumber(user.houseNumber());
    dao.zipCode(user.zipCode());
    dao.city(user.city());
    dao.country(user.country());
    dao.phoneNumber(user.phoneNumber());
    dao.aboutMe(user.aboutMe());
    // update favGenres:
    var favGenres = user.favGenres().stream()
        .map(genre -> genreRepository.findByName(genre.getName())
            .orElseThrow(() -> new BadRequestException("genreNotFound", genre.getName())))
        .toList();
    dao.favGenres(favGenres);
    userRepository.save(dao);
  }

  public User verifyLogin(User user) {
    // get salt from email:
    UserDao dao = userRepository.findByEmail(user.email())
        .orElseThrow(() -> new UserNotFoundException(user.email()));

    // check email verification status:
    if (!dao.isEmailVerified()) {
      throw new BadRequestException("userExistsButNotVerified", user.email());
    }

    // hash password with salt:
    String password = Util.hashPassword(user.password(), dao.salt());

    // validate email and password and return user:
    var verifiedUser = userRepository.findByEmailAndPassword(dao.email(), password)
        .orElseThrow(() -> new InvalidCredentials(dao.email(), password));

    return getUser(verifiedUser.id());
  }

  public void verifyCurrentPassword(User user) {
    // get salt from email:
    UserDao dao = userRepository.findByEmailAndIsEmailVerified(user.email(), true)
        .orElseThrow(() -> new UserNotFoundException(user.email()));

    // hash password with salt:
    String password = Util.hashPassword(user.password(), dao.salt());

    // validate email and password:
    if (userRepository.findByEmailAndPassword(dao.email(), password).isEmpty()) {
      throw new BadRequestException("currentPasswordMismatch", user.password());
    }
  }

  // TODO: send email to the user confirming the password change.
  public String changePassword(User user) {
    // get user from email:
    UserDao dao = userRepository.findByEmail(user.email())
        .orElseThrow(() -> new UserNotFoundException(user.email()));

    // check email verification status:
    if (!dao.isEmailVerified()) {
      throw new BadRequestException("userExistsButNotVerified", user.email());
    }

    // forbid newPassword to be the same as currentPassword:
    String currentPassword = dao.password();
    String newPassword = Util.hashPassword(user.password(), dao.salt());
    if (currentPassword.equals(newPassword)) {
      throw new BadRequestException("newPasswordCannotBeSameAsCurrentPassword", newPassword);
    }

    // add new salt to new password:
    String newSalt = Util.generateSalt();
    String newPasswordWithNewSalt = Util.hashPassword(user.password(), newSalt);

    // save password:
    dao.salt(newSalt);
    dao.password(newPasswordWithNewSalt);
    userRepository.save(dao);

    return dao.email();
  }

  // TODO: send confirmation to the verified user email.
  public String verifyEmail(String email) {
    // get user from email:
    UserDao dao = userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException(email));

    // update email verification status:
    dao.isEmailVerified(true);
    userRepository.save(dao);

    return dao.email();
  }

  public User addFavouriteBook(User user) {
    var userDao = userRepository.findByIdAndIsEmailVerifiedTrue(user.id())
        .orElseThrow(() -> new UserNotFoundException(user.email()));

    if (user.favBooks() == null || user.favBooks().isEmpty()) {
      throw new BadRequestException("favBooksListIsNullOrEmpty");
    }
    var bookId = user.favBooks().getFirst().id();
    var favBookDao = bookRepository.findByIdAndIsDeletedFalse(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    // validations:
    if (favBookDao.owner().id().equals(user.id())) {
      throw new BadRequestException("ownBookCannotBeAddedAsFavBook");
    }
    if (userDao.favBooks() != null && userDao.favBooks().stream()
        .anyMatch(book -> book.id().equals(favBookDao.id()))) {
      throw new BadRequestException("bookAlreadyExistsAsFavBook", bookId);
    }

    if (userDao.favBooks() != null)
      userDao.favBooks().add(favBookDao);
    else
      userDao.favBooks(List.of(favBookDao));

    userRepository.save(userDao);
    return getUser(user.id());
  }

  public User findOrCreateGoogleUser(String email, String firstName, String lastName, String googleSub) {
    // check if user already exists:
    var userDao = userRepository.findByEmail(email)
        .orElseGet(() -> {
          // create new user if not exists:
          var newUser = UserDao.builder()
              .email(email)
              .firstName(firstName)
              .lastName(lastName)
              .salt(googleSub)
              .isEmailVerified(true)
              .build();
          return userRepository.save(newUser);
        });

    return UserMapper.toEntity(userDao);
  }
}
