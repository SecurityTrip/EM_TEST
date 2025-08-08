package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.service.UserService;
import org.apache.http.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            logger.info("Register user with username: {}", request.getUsername());
            AuthResponse response = userService.register(request);

            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                throw new AuthenticationException("Registration failed: invalid data");
            }
        } catch (Exception e) {
            logger.error("Error during user registration", e);
            return ResponseEntity.status(500).body("Registration failed");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            logger.info("Login user with username: {}", request.getUsername());
            AuthResponse response = userService.login(request);

            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                throw new AuthenticationException("Login failed: invalid credentials");
            }
        } catch (Exception e) {
            logger.error("Error during user login", e);
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            AuthResponse response = userService.refresh(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during token refresh", e);
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
    }
}