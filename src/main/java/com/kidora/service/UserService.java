package com.kidora.service;

import com.kidora.entity.User;
import com.kidora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Value("${admin.email}")
    private String adminEmail;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
    
    public User createUser(String email, String firstName, String lastName, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already exists with email: " + email);
        }
        
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setEmailVerified(true); // Since they verified OTP
        
        // Check if admin email
        if (adminEmail.equals(email)) {
            user.setRole(User.Role.ADMIN);
        }
        
        User savedUser = userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(email, firstName);
        
        log.info("User created successfully: {}", email);
        return savedUser;
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    public boolean isAdminEmail(String email) {
        return adminEmail.equals(email);
    }
    
    public User getOrCreateUser(String email) {
        return userRepository.findByEmail(email)
                .map(existing -> {
                    // Ensure admin email always has ADMIN role, even for pre-existing users
                    if (adminEmail.equals(email) && existing.getRole() != User.Role.ADMIN) {
                        existing.setRole(User.Role.ADMIN);
                        return userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setEmailVerified(true);
                    
                    if (adminEmail.equals(email)) {
                        newUser.setRole(User.Role.ADMIN);
                    }
                    
                    return userRepository.save(newUser);
                });
    }

    public List<User> findAll() { return userRepository.findAll(); }

    public User getById(Long id) { return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found")); }
}
