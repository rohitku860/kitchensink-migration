package com.kitchensink.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
    }

    @Test
    @DisplayName("Should send email successfully")
    void testSendEmail_Success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail("recipient@example.com", "Test Subject", "Test Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should not send email when disabled")
    void testSendEmail_Disabled() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        emailService.sendEmail("recipient@example.com", "Test Subject", "Test Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send login OTP email")
    void testSendLoginOtp() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendLoginOtp("test@example.com", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send user creation email")
    void testSendUserCreationEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendUserCreationEmail("test@example.com", "Test User");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send user update notification")
    void testSendUserUpdateNotification() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendUserUpdateNotification("test@example.com", "Test User");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send update request notification")
    void testSendUpdateRequestNotification() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendUpdateRequestNotification("admin@example.com", "Test User", "name");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send update request approval")
    void testSendUpdateRequestApproval() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendUpdateRequestApproval("test@example.com", "Test User", "name");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send update request rejection")
    void testSendUpdateRequestRejection() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendUpdateRequestRejection("test@example.com", "Test User", "name", "Invalid");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send email change OTP")
    void testSendEmailChangeOtp() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmailChangeOtp("new@example.com", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send email change confirmation to both emails")
    void testSendEmailChangeConfirmation() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmailChangeConfirmation("old@example.com", "new@example.com");

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send user deletion notification")
    void testSendUserDeletionNotification() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendUserDeletionNotification("test@example.com", "Test User");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}

