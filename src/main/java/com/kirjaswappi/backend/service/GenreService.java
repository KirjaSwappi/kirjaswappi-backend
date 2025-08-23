/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kirjaswappi.backend.jpa.daos.UserDao;
import com.kirjaswappi.backend.jpa.repositories.GenreRepository;
import com.kirjaswappi.backend.jpa.repositories.UserRepository;
import com.kirjaswappi.backend.mapper.GenreMapper;
import com.kirjaswappi.backend.service.entities.Genre;
import com.kirjaswappi.backend.service.exceptions.GenreAlreadyExistsException;
import com.kirjaswappi.backend.service.exceptions.GenreCannotBeDeletedException;
import com.kirjaswappi.backend.service.exceptions.GenreNotFoundException;

@Service
@Transactional
public class GenreService {
  @Autowired
  GenreRepository genreRepository;

  @Autowired
  UserRepository userRepository;

  public List<Genre> getGenres() {
    // fetch all the genres
    return genreRepository.findAll().stream().map(GenreMapper::toEntity)
        .toList();
  }

  public Genre addGenre(Genre genre) {
    // check if the genre already exists:
    if (genreRepository.existsByName(genre.getName())) {
      throw new GenreAlreadyExistsException(genre.getName());
    }
    checkAndFetchParentIfExists(genre);
    return GenreMapper.toEntity(genreRepository.save(GenreMapper.toDao(genre)));
  }

  private void checkAndFetchParentIfExists(Genre genre) {
    // check and fetch parent if exists:
    if (genre.getParent() != null) {
      var parentGenre = genreRepository.findById(genre.getParent().getId())
          .orElseThrow(GenreNotFoundException::new);
      genre.setParent(GenreMapper.toEntity(parentGenre));
    }
  }

  public void deleteGenre(String id) {
    // Check if genre exists:
    if (!genreRepository.existsById(id)) {
      throw new GenreNotFoundException(id);
    }

    // Check if genre is associated with any user or book:
    if (isIsBeingGenreUsed(id)) {
      throw new GenreCannotBeDeletedException(id);
    }

    genreRepository.deleteById(id);
  }

  private boolean isIsBeingGenreUsed(String id) {
    return userRepository.findAll().stream().anyMatch(user -> isGenreInFavGenres(user, id) || isGenreInBooks(user, id));
  }

  private boolean isGenreInFavGenres(UserDao user, String id) {
    return user.getFavGenres() != null
        && user.getFavGenres().stream().anyMatch(favGenre -> favGenre.getId().equals(id));
  }

  private boolean isGenreInBooks(UserDao user, String id) {
    return user.getBooks() != null && user.getBooks().stream()
        .anyMatch(book -> book.getGenres().stream().anyMatch(genre -> genre.getId().equals(id)) &&
            book.getSwapCondition() != null &&
            book.getSwapCondition().getSwappableGenres().stream().anyMatch(g -> g.getId().equals(id)));
  }

  public Genre updateGenre(Genre genre) {
    var dao = genreRepository.findById(genre.getId())
        .orElseThrow(() -> new GenreNotFoundException(genre.getId()));
    dao.setName(genre.getName());
    if (genre.getParent() == null)
      dao.setParent(null);
    else {
      checkAndFetchParentIfExists(genre);
      dao.setParent(GenreMapper.toDao(genre.getParent()));
    }
    return GenreMapper.toEntity(genreRepository.save(dao));
  }

  public Genre getGenreById(String genreId) {
    var dao = genreRepository.findById(genreId)
        .orElseThrow(() -> new GenreNotFoundException(genreId));
    return GenreMapper.toEntity(dao);
  }

  public Genre getGenreByName(String genreName) {
    var dao = genreRepository.findByName(genreName)
        .orElseThrow(() -> new GenreNotFoundException(genreName));
    return GenreMapper.toEntity(dao);
  }
}
