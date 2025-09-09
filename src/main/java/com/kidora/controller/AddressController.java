package com.kidora.controller;

import com.kidora.dto.address.AddressRequest;
import com.kidora.dto.address.AddressResponse;
import com.kidora.entity.Address;
import com.kidora.entity.User;
import com.kidora.service.AddressService;
import com.kidora.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AddressController {
    
    private final AddressService addressService;
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<?> getUserAddresses(Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            List<Address> addresses = addressService.getUserAddresses(user);
            
            List<AddressResponse> response = addresses.stream()
                    .map(this::convertToAddressResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error fetching user addresses", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createAddress(@Valid @RequestBody AddressRequest request,
                                         Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            
            Address address = new Address();
            address.setFullName(request.getFullName());
            address.setPhone(request.getPhone());
            address.setStreetAddress(request.getStreetAddress());
            address.setApartment(request.getApartment());
            address.setCity(request.getCity());
            address.setState(request.getState());
            address.setPostalCode(request.getPostalCode());
            address.setCountry(request.getCountry());
            address.setDefault(request.isDefault());
            
            Address savedAddress = addressService.createAddress(user, address);
            AddressResponse response = convertToAddressResponse(savedAddress);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Address created successfully",
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error creating address", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PutMapping("/{addressId}")
    public ResponseEntity<?> updateAddress(@PathVariable Long addressId,
                                         @Valid @RequestBody AddressRequest request,
                                         Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            
            Address addressDetails = new Address();
            addressDetails.setFullName(request.getFullName());
            addressDetails.setPhone(request.getPhone());
            addressDetails.setStreetAddress(request.getStreetAddress());
            addressDetails.setApartment(request.getApartment());
            addressDetails.setCity(request.getCity());
            addressDetails.setState(request.getState());
            addressDetails.setPostalCode(request.getPostalCode());
            addressDetails.setCountry(request.getCountry());
            addressDetails.setDefault(request.isDefault());
            
            Address updatedAddress = addressService.updateAddress(user, addressId, addressDetails);
            AddressResponse response = convertToAddressResponse(updatedAddress);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Address updated successfully",
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error updating address", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long addressId,
                                         Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            addressService.deleteAddress(user, addressId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Address deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting address", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/default")
    public ResponseEntity<?> getDefaultAddress(Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            Address defaultAddress = addressService.getDefaultAddress(user);
            
            if (defaultAddress == null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", null,
                    "message", "No default address found"
                ));
            }
            
            AddressResponse response = convertToAddressResponse(defaultAddress);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error fetching default address", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    private AddressResponse convertToAddressResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setFullName(address.getFullName());
        response.setPhone(address.getPhone());
        response.setStreetAddress(address.getStreetAddress());
        response.setApartment(address.getApartment());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setPostalCode(address.getPostalCode());
        response.setCountry(address.getCountry());
        response.setDefault(address.isDefault());
        response.setCreatedAt(address.getCreatedAt());
        response.setUpdatedAt(address.getUpdatedAt());
        return response;
    }
}
