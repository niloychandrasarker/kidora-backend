package com.kidora.controller;

import com.kidora.dto.user.UpdateUserRequest;
import com.kidora.dto.user.UserProfileResponse;
import com.kidora.entity.User;
import com.kidora.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            UserProfileResponse resp = new UserProfileResponse(
                    user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhone(), user.getRole().name()
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", resp
            ));
        } catch (Exception e) {
            log.error("Error fetching profile", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody UpdateUserRequest req, Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
            if (req.getLastName() != null) user.setLastName(req.getLastName());
            if (req.getPhone() != null) user.setPhone(req.getPhone());
            user = userService.updateUser(user);
            UserProfileResponse resp = new UserProfileResponse(
                    user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhone(), user.getRole().name()
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated",
                    "data", resp
            ));
        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
