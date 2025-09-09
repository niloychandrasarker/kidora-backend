package com.kidora.repository;

import com.kidora.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByEmailAndOtpCodeAndUsedFalseAndExpiryTimeAfter(
        String email, String otpCode, LocalDateTime currentTime);
    
    void deleteByEmailAndType(String email, Otp.OtpType type);
    
    void deleteByExpiryTimeBefore(LocalDateTime currentTime);
}
