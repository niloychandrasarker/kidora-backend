package com.kidora.dto.address;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AddressResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String streetAddress;
    private String apartment;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
