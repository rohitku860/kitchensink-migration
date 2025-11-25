package com.kitchensink.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:rohitku860@gmail.com}")
    private String fromEmail;
    
    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Async
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            logger.warn("Email sending is disabled. Would send to: {}, subject: {}", to, subject);
            return;
        }
        
        if (mailSender == null) {
            logger.warn("JavaMailSender is not configured. Would send to: {}, subject: {}", to, subject);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            // Don't throw exception - email failure shouldn't break the flow
        }
    }
    
    @Async
    public void sendLoginOtp(String email, String otp) {
        String subject = "Your Login OTP";
        String body = String.format(
            "Hello,\n\n" +
            "Your login OTP is: %s\n\n" +
            "This OTP is valid for 10 minutes.\n\n" +
            "If you did not request this OTP, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Kitchensink Team",
            otp
        );
        sendEmail(email, subject, body);
    }
    
    @Async
    public void sendUserCreationEmail(String email, String name) {
        String subject = "Welcome to Kitchensink!";
        String body = String.format(
            "Hello %s,\n\n" +
            "Your account has been created successfully.\n\n" +
            "You can now log in using your email address and OTP.\n\n" +
            "Best regards,\n" +
            "Kitchensink Team",
            name
        );
        sendEmail(email, subject, body);
    }
    
    @Async
    public void sendUserUpdateNotification(String email, String name) {
        String subject = "Your Profile Has Been Updated";
        String body = String.format(
            "Hello %s,\n\n" +
            "Your profile has been updated by an administrator.\n\n" +
            "The following changes have been made to your account:\n" +
            "- Your profile information has been modified\n\n" +
            "Please log in to view your updated profile and verify the changes.\n\n" +
            "If you did not authorize these changes, please contact support immediately.\n\n" +
            "Best regards,\n" +
            "Kitchensink Team",
            name
        );
        sendEmail(email, subject, body);
    }
    
    @Async
    public void sendUpdateRequestNotification(String adminEmail, String userName, String fieldName) {
        String subject = "New Update Request from User";
        String body = String.format(
            "Hello Admin,\n\n" +
            "User %s has requested an update to their %s field.\n\n" +
            "Please review the request in the admin dashboard.\n\n" +
            "Best regards,\n" +
            "Kitchensink System",
            userName, fieldName
        );
        sendEmail(adminEmail, subject, body);
    }
    
    @Async
    public void sendUpdateRequestApproval(String email, String name, String fieldName) {
        String subject = "Your Update Request Has Been Approved";
        String body = String.format(
            "Hello %s,\n\n" +
            "Your update request for %s has been approved.\n\n" +
            "Your profile has been updated accordingly.\n\n" +
            "Best regards,\n" +
            "Kitchensink Team",
            name, fieldName
        );
        sendEmail(email, subject, body);
    }
    
    @Async
    public void sendUpdateRequestRejection(String email, String name, String fieldName, String reason) {
        String subject = "Your Update Request Has Been Rejected";
        String body = String.format(
            "Hello %s,\n\n" +
            "Your update request for %s has been rejected.\n\n" +
            "Reason: %s\n\n" +
            "If you have any questions, please contact support.\n\n" +
            "Best regards,\n" +
            "Kitchensink Team",
            name, fieldName, reason
        );
        sendEmail(email, subject, body);
    }
    
    @Async
    public void sendEmailChangeOtp(String email, String otp) {
        String subject = "Email Change Verification OTP";
        String body = String.format(
            "Hello,\n\n" +
            "You have requested to change your email address.\n\n" +
            "Your verification OTP is: %s\n\n" +
            "This OTP is valid for 10 minutes.\n\n" +
            "If you did not request this change, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Kitchensink Team",
            otp
        );
        sendEmail(email, subject, body);
    }
    
    @Async
    public void sendEmailChangeConfirmation(String oldEmail, String newEmail) {
        String subject = "Email Address Changed Successfully";
        String body = String.format(
            "Hello,\n\n" +
            "Your email address has been successfully changed.\n\n" +
            "Old email: %s\n" +
            "New email: %s\n\n" +
            "You can now use your new email address to log in.\n\n" +
            "Best regards,\n" +
            "Kitchensink Team",
            oldEmail, newEmail
        );
        
        // Send to both old and new email
        sendEmail(oldEmail, subject, body);
        sendEmail(newEmail, subject, body);
    }
    
    @Async
    public void sendUserDeletionNotification(String email, String name) {
        String subject = "Account Deletion Notification";
        String body = String.format(
            "Hello %s,\n\n" +
            "Your account has been deleted by an administrator.\n\n" +
            "If you believe this was done in error, please contact support.\n\n" +
            "Best regards,\n" +
            "Kitchensink Team",
            name
        );
        sendEmail(email, subject, body);
    }
}

