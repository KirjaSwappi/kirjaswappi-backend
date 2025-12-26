/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.common.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * Service responsible for sending emails, including OTP verification emails.
 */
@Service
@Async
@RequiredArgsConstructor
public class EmailService {
  private final JavaMailSender mailSender;
  private final Environment env;
  private final Logger logger = LoggerFactory.getLogger(EmailService.class);

  @PostConstruct
  public void validateConfiguration() {
    String senderEmail = env.getProperty("spring.mail.from-email");
    if (senderEmail == null || senderEmail.trim().isEmpty()) {
      logger.warn("Email sender address is not configured. Emails will not be sent properly.");
    }
  }

  /**
   * Sends an OTP verification code to the specified email address.
   *
   * @param email The recipient's email address
   * @param otp   The one-time password to be sent
   */
  public void sendOTPByEmail(String email, String otp) {
    String subject = "OTP Verification";
    try {
      String template = loadEmailTemplate();
      String emailText = template.replace("{{otp}}", otp);
      sendEmail(email, subject, emailText);
    } catch (IOException e) {
      logger.error("Failed to load email template: {}", e.getMessage(), e);
    } catch (Exception e) {
      logger.error("Unexpected error while sending OTP email to {}: {}", email, e.getMessage(), e);
    }
  }

  /**
   * Sends a confirmation email after a password change.
   *
   * @param email The user's email address
   */
  public void sendPasswordChangeConfirmation(String email) {
    String subject = "Password Changed Successfully";
    try {
      String template = loadGenericEmailTemplate();
      String content = "<h2>Password Updated</h2>"
          + "<p>Hello,</p>"
          + "<p>This is a confirmation that your KirjaSwappi account password has been changed successfully.</p>"
          + "<p>If you did not perform this action, please contact support immediately.</p>"
          + "<p>Best regards,<br/>The KirjaSwappi Team</p>";
      String emailText = template.replace("{{title}}", subject).replace("{{content}}", content);
      sendEmail(email, subject, emailText);
    } catch (IOException e) {
      logger.error("Failed to load generic email template: {}", e.getMessage(), e);
    }
  }

  /**
   * Sends a confirmation email after successful email verification.
   *
   * @param email The user's email address
   */
  public void sendEmailVerificationConfirmation(String email) {
    String subject = "Email Verified Successfully";
    try {
      String template = loadGenericEmailTemplate();
      String content = "<h2>Welcome to KirjaSwappi!</h2>"
          + "<p>Hello,</p>"
          + "<p>Your email address has been successfully verified. You can now use all features of KirjaSwappi.</p>"
          + "<p>Happy book swapping!</p>"
          + "<p>Best regards,<br/>The KirjaSwappi Team</p>";
      String emailText = template.replace("{{title}}", subject).replace("{{content}}", content);
      sendEmail(email, subject, emailText);
    } catch (IOException e) {
      logger.error("Failed to load generic email template: {}", e.getMessage(), e);
    }
  }

  private String loadGenericEmailTemplate() throws IOException {
    Resource resource = new ClassPathResource("templates/GenericEmailTemplate.html");
    return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
  }

  /**
   * Loads the email template from the resources directory.
   *
   * @return The email template as a string
   * @throws IOException If the template cannot be read
   */
  private String loadEmailTemplate() throws IOException {
    Resource resource = new ClassPathResource("templates/EmailTemplate.html");
    if (!resource.exists()) {
      logger.error("Email template file does not exist at location: templates/EmailTemplate.html");
      throw new IOException("Email template file not found");
    }

    try (var inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Error reading email template file: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Sends an email with HTML content.
   *
   * @param to       The recipient's email address
   * @param subject  The email subject
   * @param htmlBody The HTML content of the email
   */
  private void sendEmail(String to, String subject, String htmlBody) {
    if (to == null || to.trim().isEmpty()) {
      logger.error("Cannot send email: recipient address is empty");
      return;
    }

    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      String from = Objects.requireNonNull(env.getProperty("spring.mail.from-email"), "Sender email is not configured");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      mailSender.send(message);
      logger.info("Email sent successfully to: {}", to);
    } catch (MessagingException e) {
      logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
    } catch (NullPointerException e) {
      logger.error("Email configuration error: {}", e.getMessage(), e);
    } catch (Exception e) {
      logger.error("Unexpected error while sending email to {}: {}", to, e.getMessage(), e);
    }
  }
}
