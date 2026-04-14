/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.integration;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirjaswappi.backend.common.http.controllers.mockMvc.config.CustomMockMvcConfiguration;
import com.kirjaswappi.backend.common.service.EmailService;
import com.kirjaswappi.backend.http.controllers.FormController;
import com.kirjaswappi.backend.http.dtos.requests.FormSubmissionRequest;

@WebMvcTest(FormController.class)
@Import(CustomMockMvcConfiguration.class)
class FormApiIntegrationTest {

  private static final String API_PATH = "/api/v1/forms";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private EmailService emailService;

  private FormSubmissionRequest validRequest() {
    FormSubmissionRequest request = new FormSubmissionRequest();
    request.setName("John Doe");
    request.setEmail("john@example.com");
    request.setMessage("I would like to get in touch.");
    return request;
  }

  @Nested
  @DisplayName("Form Type Tests")
  class FormTypeTests {

    @Test
    @DisplayName("Should submit contact form")
    void shouldSubmitContactForm() throws Exception {
      doNothing().when(emailService).sendFormSubmission(
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

      mockMvc.perform(post(API_PATH + "/contact")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should submit collaboration form")
    void shouldSubmitCollaborationForm() throws Exception {
      doNothing().when(emailService).sendFormSubmission(
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

      mockMvc.perform(post(API_PATH + "/collaboration")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should submit donation form")
    void shouldSubmitDonationForm() throws Exception {
      doNothing().when(emailService).sendFormSubmission(
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

      mockMvc.perform(post(API_PATH + "/donation")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should submit feedback form")
    void shouldSubmitFeedbackForm() throws Exception {
      doNothing().when(emailService).sendFormSubmission(
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

      mockMvc.perform(post(API_PATH + "/feedback")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should submit volunteer form")
    void shouldSubmitVolunteerForm() throws Exception {
      doNothing().when(emailService).sendFormSubmission(
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

      mockMvc.perform(post(API_PATH + "/volunteer")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 for invalid form type")
    void shouldReturn400ForInvalidFormType() throws Exception {
      mockMvc.perform(post(API_PATH + "/invalid")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should return 400 when name is missing")
    void shouldReturn400WhenNameMissing() throws Exception {
      FormSubmissionRequest request = validRequest();
      request.setName(null);

      mockMvc.perform(post(API_PATH + "/contact")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void shouldReturn400WhenEmailInvalid() throws Exception {
      FormSubmissionRequest request = validRequest();
      request.setEmail("not-an-email");

      mockMvc.perform(post(API_PATH + "/contact")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email is missing")
    void shouldReturn400WhenEmailMissing() throws Exception {
      FormSubmissionRequest request = validRequest();
      request.setEmail(null);

      mockMvc.perform(post(API_PATH + "/contact")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when message is missing")
    void shouldReturn400WhenMessageMissing() throws Exception {
      FormSubmissionRequest request = validRequest();
      request.setMessage(null);

      mockMvc.perform(post(API_PATH + "/contact")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }
}
