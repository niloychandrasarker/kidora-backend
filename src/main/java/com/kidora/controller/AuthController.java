package com.kidora.controller;

import com.kidora.dto.auth.*;
import com.kidora.entity.Otp;
import com.kidora.entity.User;
import com.kidora.service.OtpService;
import com.kidora.service.UserService;
import com.kidora.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final UserService userService;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody LoginRequest request) {
        try {
            Otp.OtpType otpType = userService.isAdminEmail(request.getEmail()) 
                ? Otp.OtpType.ADMIN_LOGIN 
                : Otp.OtpType.LOGIN;
            
            otpService.generateAndSendOtp(request.getEmail(), otpType);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully to your email"
            ));
        } catch (Exception e) {
            log.error("Error sending OTP", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to send OTP. Please try again."
            ));
        }
    }
    
    @PostMapping("/register/send-otp")
    public ResponseEntity<?> sendRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        try {
            if (userService.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User already exists with this email. Please login instead."
                ));
            }
            
            otpService.generateAndSendOtp(request.getEmail(), Otp.OtpType.REGISTRATION);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully to your email"
            ));
        } catch (Exception e) {
            log.error("Error sending registration OTP", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to send OTP. Please try again."
            ));
        }
    }
    
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
            
            if (!isValid) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid or expired OTP"
                ));
            }
            
            // Get or create user
            User user = userService.getOrCreateUser(request.getEmail());
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            
            AuthResponse response = new AuthResponse(
                token,
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() == User.Role.ADMIN
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful",
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error verifying OTP", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to verify OTP. Please try again."
            ));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.createUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone()
            );
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            
            AuthResponse response = new AuthResponse(
                token,
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() == User.Role.ADMIN
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Registration successful",
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error during registration", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid token format"
                ));
            }
            
            String token = authHeader.substring(7);
            boolean isValid = jwtUtil.validateToken(token);
            
            if (!isValid) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid or expired token"
                ));
            }
            
            String email = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "email", email,
                    "role", role,
                    "isAdmin", "ADMIN".equals(role)
                )
            ));
        } catch (Exception e) {
            log.error("Error validating token", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid token"
            ));
        }
    }
}
