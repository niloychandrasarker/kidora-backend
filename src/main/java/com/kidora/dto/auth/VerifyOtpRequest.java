package com.kidora.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    
    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    @NotBlank(message = "OTP is required")
    private String otp;
}
