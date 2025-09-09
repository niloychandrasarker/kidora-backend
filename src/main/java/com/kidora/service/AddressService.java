package com.kidora.service;

import com.kidora.entity.Address;
import com.kidora.entity.User;
import com.kidora.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressService {
    
    private final AddressRepository addressRepository;
    
    public List<Address> getUserAddresses(User user) {
        return addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
    }
    
    public Address createAddress(User user, Address address) {
        address.setUser(user);
        
        // If this is the first address, make it default
        if (addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user).isEmpty()) {
            address.setDefault(true);
        }
        
        // If setting as default, remove default from other addresses
        if (address.isDefault()) {
            setOthersAsNonDefault(user);
        }
        
        return addressRepository.save(address);
    }
    
    public Address updateAddress(User user, Long addressId, Address addressDetails) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to update this address");
        }
        
        address.setFullName(addressDetails.getFullName());
        address.setPhone(addressDetails.getPhone());
        address.setStreetAddress(addressDetails.getStreetAddress());
        address.setApartment(addressDetails.getApartment());
        address.setCity(addressDetails.getCity());
        address.setState(addressDetails.getState());
        address.setPostalCode(addressDetails.getPostalCode());
        address.setCountry(addressDetails.getCountry());
        address.setUpdatedAt(LocalDateTime.now());
        
        // If setting as default, remove default from other addresses
        if (addressDetails.isDefault() && !address.isDefault()) {
            setOthersAsNonDefault(user);
            address.setDefault(true);
        }
        
        return addressRepository.save(address);
    }
    
    public void deleteAddress(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this address");
        }
        
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);
        
        // If deleted address was default, make another one default
        if (wasDefault) {
            List<Address> remainingAddresses = addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
            if (!remainingAddresses.isEmpty()) {
                Address newDefault = remainingAddresses.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }
        
        log.info("Address deleted for user: {}", user.getEmail());
    }
    
    private void setOthersAsNonDefault(User user) {
        List<Address> userAddresses = addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
        userAddresses.forEach(addr -> {
            if (addr.isDefault()) {
                addr.setDefault(false);
                addressRepository.save(addr);
            }
        });
    }
    
    public Address getDefaultAddress(User user) {
        return addressRepository.findByUserAndIsDefaultTrue(user).orElse(null);
    }
}
