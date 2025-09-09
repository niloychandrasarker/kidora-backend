package com.kidora.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender javaMailSender;
    
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@kidora.com");
            message.setTo(toEmail);
            message.setSubject("Kidora - OTP Verification");
            
            String emailBody = String.format(
                "Dear User,\n\n" +
                "Your OTP for %s is: %s\n\n" +
                "This OTP will expire in 5 minutes.\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Kidora Team",
                purpose, otp
            );
            
            message.setText(emailBody);
            javaMailSender.send(message);
            
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email");
        }
    }
    
    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@kidora.com");
            message.setTo(toEmail);
            message.setSubject("Welcome to Kidora!");
            
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Welcome to Kidora! Your account has been successfully created.\n\n" +
                "You can now start shopping for amazing kids' products.\n\n" +
                "Happy Shopping!\n\n" +
                "Best regards,\n" +
                "Kidora Team",
                name != null ? name : "Customer"
            );
            
            message.setText(emailBody);
            javaMailSender.send(message);
            
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }
}
