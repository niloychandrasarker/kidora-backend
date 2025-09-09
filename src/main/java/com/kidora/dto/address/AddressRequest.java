package com.kidora.dto.address;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    @NotBlank(message = "Street address is required")
    private String streetAddress;
    
    private String apartment;
    
    @NotBlank(message = "City is required")
    private String city;
    
    private String state;
    
    @NotBlank(message = "Postal code is required")
    private String postalCode;
    
    private String country = "Bangladesh";
    
    private boolean isDefault = false;
}
