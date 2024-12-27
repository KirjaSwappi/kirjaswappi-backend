/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.http;

import static com.kirjaswappi.backend.common.utils.PathProvider.getCurrentPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.kirjaswappi.backend.common.exceptions.BusinessException;
import com.kirjaswappi.backend.common.exceptions.SystemException;
import com.kirjaswappi.backend.common.service.exceptions.InvalidCredentials;
import com.kirjaswappi.backend.service.exceptions.ResourceNotFoundException;
import com.kirjaswappi.backend.service.exceptions.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @Autowired
  private ErrorUtils errorUtils;

  private static final String INTERNAL_ERROR = "internalServerError";
  private static final String PATH_NOT_FOUND = "pathNotFound";

  @ExceptionHandler(InvalidCredentials.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponse handleInvalidCredentialsException(InvalidCredentials exception, WebRequest webRequest) {
    return errorUtils.buildErrorResponse(exception, webRequest);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException exception, WebRequest webRequest) {
    return errorUtils.buildErrorResponse(exception, webRequest);
  }

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleUserNotFoundException(UserNotFoundException exception, WebRequest webRequest) {
    return errorUtils.buildErrorResponse(exception, webRequest);
  }

  @ExceptionHandler(SystemException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleGenericSystemException(SystemException exception, WebRequest webRequest) {
    return new ErrorResponse(new ErrorResponse.Error(exception.getCode(),
        errorUtils.resolveMessage(INTERNAL_ERROR, null, webRequest.getLocale())));
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleNoHandlerFoundException(WebRequest webRequest) {
    return new ErrorResponse(new ErrorResponse.Error(PATH_NOT_FOUND,
        errorUtils.resolveMessage(PATH_NOT_FOUND, null, webRequest.getLocale()), getCurrentPath()));
  }

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleGenericBusinessException(BusinessException exception, WebRequest webRequest) {
    return errorUtils.buildErrorResponse(exception, webRequest);
  }
}
