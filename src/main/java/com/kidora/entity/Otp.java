package com.kidora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Otp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String otpCode;
    
    @Column(nullable = false)
    private LocalDateTime expiryTime;
    
    private boolean used = false;
    
    @Enumerated(EnumType.STRING)
    private OtpType type;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum OtpType {
        LOGIN, REGISTRATION, ADMIN_LOGIN
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
    
    public boolean isValid() {
        return !used && !isExpired();
    }
}
