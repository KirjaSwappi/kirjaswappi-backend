/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.common.service.exceptions.InvalidCredentials;
import com.kirjaswappi.backend.common.utils.Util;
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
public class UserService {
  @Autowired
  UserRepository userRepository;
  @Autowired
  GenreRepository genreRepository;
  @Autowired
  PhotoService photoService;
  @Autowired
  private BookRepository bookRepository;

  public User addUser(User user) {
    this.checkUserExistButNotVerified(user);
    this.checkIfUserAlreadyExists(user);

    // add salt to password:
    String salt = Util.generateSalt();
    user.setPassword(user.getPassword(), salt);

    // save user:
    UserDao dao = UserMapper.toDao(user, salt);
    dao.setEmailVerified(false);
    return UserMapper.toEntity(userRepository.save(dao));
  }

  public boolean checkIfUserExists(String email) {
    return userRepository.findByEmail(email).isPresent();
  }

  private void checkIfUserAlreadyExists(User user) {
    if (userRepository.findByEmailAndIsEmailVerified(user.getEmail(), true).isPresent()) {
      throw new UserAlreadyExistsException(user.getEmail());
    }
  }

  private void checkUserExistButNotVerified(User user) {
    // validate user exists and email is not verified:
    if (userRepository.findByEmailAndIsEmailVerified(user.getEmail(), false).isPresent()) {
      throw new BadRequestException("userExistsButNotVerified", user.getEmail());
    }
  }

  public User getUser(String id) {
    var userDao = userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
    if (userDao.getBooks() != null) {
      userDao.getBooks().forEach(bookDao -> {
        var imageUrl = photoService.getBookCoverPhoto(bookDao.getCoverPhoto());
        bookDao.setCoverPhoto(imageUrl);
      });
    }
    return UserMapper.toEntity(userDao);
  }

  public List<User> getUsers() {
    return userRepository.findAll().stream().map(userDao -> {
      if (userDao.getBooks() != null) {
        userDao.getBooks().forEach(bookDao -> {
          var imageUrl = photoService.getBookCoverPhoto(bookDao.getCoverPhoto());
          bookDao.setCoverPhoto(imageUrl);
        });
      }
      return UserMapper.toEntity(userDao);
    }).toList();
  }

  public void deleteUser(String id) {
    userRepository.deleteById(id);
  }

  public User updateUser(User user) {
    // validate user exists:
    var dao = userRepository.findById(user.getId())
        .orElseThrow(() -> new UserNotFoundException(user.getId()));

    // check email verification status:
    if (!dao.isEmailVerified()) {
      throw new BadRequestException("userExistsButNotVerified", user.getEmail());
    }

    // update user details:
    updateUserDetails(user, dao);

    return getUser(user.getId());
  }

  private void updateUserDetails(User user, UserDao dao) {
    dao.setFirstName(user.getFirstName());
    dao.setLastName(user.getLastName());
    dao.setStreetName(user.getStreetName());
    dao.setHouseNumber(user.getHouseNumber());
    dao.setZipCode(user.getZipCode());
    dao.setCity(user.getCity());
    dao.setCountry(user.getCountry());
    dao.setPhoneNumber(user.getPhoneNumber());
    dao.setAboutMe(user.getAboutMe());
    // update favGenres:
    var favGenres = user.getFavGenres().stream()
        .map(genre -> genreRepository.findByName(genre)
            .orElseThrow(() -> new BadRequestException("genreNotFound", genre)))
        .toList();
    dao.setFavGenres(favGenres);
    userRepository.save(dao);
  }

  public User verifyLogin(User user) {
    // get salt from email:
    UserDao dao = userRepository.findByEmail(user.getEmail())
        .orElseThrow(() -> new UserNotFoundException(user.getEmail()));

    // check email verification status:
    if (!dao.isEmailVerified()) {
      throw new BadRequestException("userExistsButNotVerified", user.getEmail());
    }

    // hash password with salt:
    String password = Util.hashPassword(user.getPassword(), dao.getSalt());

    // validate email and password and return user:
    var verifiedUser = userRepository.findByEmailAndPassword(dao.getEmail(), password)
        .orElseThrow(() -> new InvalidCredentials(dao.getEmail(), password));

    return getUser(verifiedUser.getId());
  }

  public void verifyCurrentPassword(User user) {
    // get salt from email:
    UserDao dao = userRepository.findByEmail(user.getEmail())
        .orElseThrow(() -> new UserNotFoundException(user.getEmail()));

    // hash password with salt:
    String password = Util.hashPassword(user.getPassword(), dao.getSalt());

    // validate email and password:
    if (userRepository.findByEmailAndPassword(dao.getEmail(), password).isEmpty()) {
      throw new BadRequestException("currentPasswordMismatch", user.getPassword());
    }
  }

  public String changePassword(User user) {
    // get user from email:
    UserDao dao = userRepository.findByEmail(user.getEmail())
        .orElseThrow(() -> new UserNotFoundException(user.getEmail()));

    // forbid newPassword to be the same as currentPassword:
    String currentPassword = dao.getPassword();
    String newPassword = Util.hashPassword(user.getPassword(), dao.getSalt());
    if (currentPassword.equals(newPassword)) {
      throw new BadRequestException("newPasswordCannotBeSameAsCurrentPassword", newPassword);
    }

    // add new salt to new password:
    String newSalt = Util.generateSalt();
    String newPasswordWithNewSalt = Util.hashPassword(user.getPassword(), newSalt);

    // save password:
    dao.setSalt(newSalt);
    dao.setPassword(newPasswordWithNewSalt);
    userRepository.save(dao);

    return dao.getEmail();
  }

  public String verifyEmail(String email) {
    // get user from email:
    UserDao dao = userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException(email));

    // update email verification status:
    dao.setEmailVerified(true);
    userRepository.save(dao);

    return dao.getEmail();
  }

  public User addFavouriteBook(User user) {
    var userDao = userRepository.findById(user.getId()).orElseThrow(() -> new UserNotFoundException(user.getEmail()));

    assert user.getFavBooks() != null;
    var bookId = user.getFavBooks().get(0).getId();
    var favBookDao = bookRepository.findByIdAndIsDeletedFalse(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    // validations:
    if (favBookDao.getOwner().getId().equals(user.getId())) {
      throw new BadRequestException("ownBookCannotBeAddedAsFavBook");
    }
    if (userDao.getFavBooks() != null && userDao.getFavBooks().stream()
        .anyMatch(book -> book.getId().equals(favBookDao.getId()))) {
      throw new BadRequestException("bookAlreadyExistsAsFavBook", bookId);
    }

    if (userDao.getFavBooks() != null)
      userDao.getFavBooks().add(favBookDao);
    else
      userDao.setFavBooks(List.of(favBookDao));

    userRepository.save(userDao);
    return getUser(user.getId());
  }
}
