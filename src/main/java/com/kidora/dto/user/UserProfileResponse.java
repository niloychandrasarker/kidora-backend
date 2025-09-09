package com.kidora.dto.user;

public record UserProfileResponse(
        String email,
        String firstName,
        String lastName,
        String phone,
        String role
) {}
