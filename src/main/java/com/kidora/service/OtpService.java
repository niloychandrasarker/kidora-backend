package com.kidora.service;

import com.kidora.entity.Otp;
import com.kidora.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OtpService {
    
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    
    @Value("${otp.expiration}")
    private Long otpExpiration;
    
    private final Random random = new Random();
    
    public String generateAndSendOtp(String email, Otp.OtpType type) {
        // Delete any existing OTPs for this email and type
        otpRepository.deleteByEmailAndType(email, type);
        
        // Generate 6-digit OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));
        
        // Create OTP entity
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setType(type);
        otp.setExpiryTime(LocalDateTime.now().plusSeconds(otpExpiration / 1000));
        
        otpRepository.save(otp);
        
        // Send email asynchronously
        sendOtpEmailAsync(email, otpCode, type);
        
        log.info("OTP generated for email: {} with type: {}", email, type);
        return otpCode;
    }
    
    @Async
    public void sendOtpEmailAsync(String email, String otp, Otp.OtpType type) {
        String purpose = switch (type) {
            case LOGIN -> "login";
            case REGISTRATION -> "registration";
            case ADMIN_LOGIN -> "admin login";
        };
        emailService.sendOtpEmail(email, otp, purpose);
    }
    
    public boolean verifyOtp(String email, String otpCode) {
        var otpOpt = otpRepository.findByEmailAndOtpCodeAndUsedFalseAndExpiryTimeAfter(
            email, otpCode, LocalDateTime.now());
        
        if (otpOpt.isPresent()) {
            Otp otp = otpOpt.get();
            otp.setUsed(true);
            otpRepository.save(otp);
            log.info("OTP verified successfully for email: {}", email);
            return true;
        }
        
        log.warn("OTP verification failed for email: {}", email);
        return false;
    }
    
    // Clean up expired OTPs every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
        log.info("Cleaned up expired OTPs");
    }
}
