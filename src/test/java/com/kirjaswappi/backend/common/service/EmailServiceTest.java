/*
 * Copyright (c) 2025 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@SuppressWarnings("unused")
class EmailServiceTest {
  @Mock
  private JavaMailSender mailSender;
  @Mock
  private Environment env;

  private EmailService emailService;
  private Method loadEmailTemplateMethod;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    emailService = new EmailService(mailSender, env);

    // Get reference to the private method
    loadEmailTemplateMethod = EmailService.class.getDeclaredMethod("loadEmailTemplate");
    loadEmailTemplateMethod.setAccessible(true);
  }

  @Test
  @DisplayName("Should validate configuration and log warning when email sender not configured")
  void validateConfigurationLogWarningWhenNoSender() {
    when(env.getProperty("spring.mail.from-email")).thenReturn(null);
    emailService.validateConfiguration();
    // This test verifies that the method executes without exception
    // Ideally we would test for log output but that requires additional setup
  }

  @Test
  @DisplayName("Should validate configuration without warning when email sender is configured")
  void validateConfigurationSuccessWhenSenderConfigured() {
    when(env.getProperty("spring.mail.from-email")).thenReturn("test@example.com");
    emailService.validateConfiguration();
    // No exception means success
  }

  @Test
  @DisplayName("Should load email template successfully")
  void loadEmailTemplateSuccess() throws Exception {
    // Create a mock Resource to return for ClassPathResource
    Resource mockResource = mock(Resource.class);
    ClassPathResource mockClassPathResource = mock(ClassPathResource.class);

    // Set up mock to return our mock resource
    when(mockClassPathResource.exists()).thenReturn(true);
    when(mockClassPathResource.getInputStream()).thenReturn(
        new ByteArrayInputStream("Template with {{otp}}".getBytes(StandardCharsets.UTF_8)));

    // Replace the ClassPathResource creation in the method
    try (MockedConstruction<ClassPathResource> mockedConstruction = mockConstruction(ClassPathResource.class,
        (mock, context) -> {
          when(mock.exists()).thenReturn(true);
          when(mock.getInputStream()).thenReturn(
              new ByteArrayInputStream("Template with {{otp}}".getBytes(StandardCharsets.UTF_8)));
        })) {

      String template = (String) loadEmailTemplateMethod.invoke(emailService);
      assertNotNull(template);
      assertEquals("Template with {{otp}}", template);
    }
  }

  @Test
  @DisplayName("Should throw IOException when template file doesn't exist")
  void loadEmailTemplateThrowsWhenFileNotExists() throws Exception {
    try (MockedConstruction<ClassPathResource> mockedConstruction = mockConstruction(ClassPathResource.class,
        (mock, context) -> {
          when(mock.exists()).thenReturn(false);
        })) {

      try {
        loadEmailTemplateMethod.invoke(emailService);
        fail("Expected exception was not thrown");
      } catch (InvocationTargetException e) {
        // When using reflection, the actual exception is wrapped in
        // InvocationTargetException
        assertTrue(e.getCause() instanceof IOException);
        assertEquals("Email template file not found", e.getCause().getMessage());
      }
    }
  }

  @Test
  @DisplayName("Should send email successfully (private method)")
  void sendEmailSuccess() throws Exception {
    MimeMessage message = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(message);
    when(env.getProperty("spring.mail.from-email")).thenReturn("from@example.com");
    doNothing().when(mailSender).send(any(MimeMessage.class));

    // Use reflection to access the private method
    Method sendEmailMethod = EmailService.class.getDeclaredMethod("sendEmail", String.class, String.class,
        String.class);
    sendEmailMethod.setAccessible(true);

    assertDoesNotThrow(() -> sendEmailMethod.invoke(emailService, "to@example.com", "Subject", "<b>body</b>"));
    verify(mailSender, times(1)).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("Should not throw exception when recipient is empty")
  void sendEmailHandlesEmptyRecipient() throws Exception {
    // Use reflection to access the private method
    Method sendEmailMethod = EmailService.class.getDeclaredMethod("sendEmail", String.class, String.class,
        String.class);
    sendEmailMethod.setAccessible(true);

    assertDoesNotThrow(() -> sendEmailMethod.invoke(emailService, "", "Subject", "<b>body</b>"));
    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("Should handle null from address")
  void sendEmailHandlesNullFrom() throws Exception {
    MimeMessage message = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(message);
    when(env.getProperty("spring.mail.from-email")).thenReturn(null);

    // Use reflection to access the private method
    Method sendEmailMethod = EmailService.class.getDeclaredMethod("sendEmail", String.class, String.class,
        String.class);
    sendEmailMethod.setAccessible(true);

    // The method should handle the null sender gracefully
    assertDoesNotThrow(() -> sendEmailMethod.invoke(emailService, "to@example.com", "Subject", "<b>body</b>"));

    // Verify that no email was sent
    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("Should handle MessagingException gracefully")
  void sendEmailHandlesMessagingException() throws Exception {
    MimeMessage message = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(message);
    when(env.getProperty("spring.mail.from-email")).thenReturn("from@example.com");

    // Simulate a MessagingException when sending the email
    doThrow(new MailSendException("Failed to send email")).when(mailSender).send(any(MimeMessage.class));

    // Use reflection to access the private method
    Method sendEmailMethod = EmailService.class.getDeclaredMethod("sendEmail", String.class, String.class,
        String.class);
    sendEmailMethod.setAccessible(true);

    // This should not throw an exception as the method handles MessagingException
    // internally
    assertDoesNotThrow(() -> sendEmailMethod.invoke(emailService, "to@example.com", "Subject", "<b>body</b>"));
  }

  @Test
  @DisplayName("Should send OTP email successfully")
  void sendOTPByEmailSuccess() throws Exception {
    // Set up to return mock template when loadEmailTemplate is invoked via
    // reflection
    try (MockedConstruction<ClassPathResource> mockedConstruction = mockConstruction(ClassPathResource.class,
        (mock, context) -> {
          when(mock.exists()).thenReturn(true);
          when(mock.getInputStream()).thenReturn(
              new ByteArrayInputStream("Template with {{otp}}".getBytes(StandardCharsets.UTF_8)));
        })) {

      MimeMessage message = mock(MimeMessage.class);
      when(mailSender.createMimeMessage()).thenReturn(message);
      when(env.getProperty("spring.mail.from-email")).thenReturn("from@example.com");
      doNothing().when(mailSender).send(any(MimeMessage.class));

      assertDoesNotThrow(() -> emailService.sendOTPByEmail("to@example.com", "123456"));
    }
  }

  @Test
  @DisplayName("Should handle IOException when loading template in sendOTPByEmail")
  void sendOTPByEmailHandlesIOException() throws Exception {
    EmailService spyService = spy(emailService);

    // Create a method spy that throws IOException when loadEmailTemplate is called
    try (MockedConstruction<ClassPathResource> mockedConstruction = mockConstruction(ClassPathResource.class,
        (mock, context) -> {
          when(mock.exists()).thenReturn(false);
        })) {

      // This should not throw an exception as the method handles IOException
      // internally
      assertDoesNotThrow(() -> spyService.sendOTPByEmail("to@example.com", "123456"));
    }
  }
}
