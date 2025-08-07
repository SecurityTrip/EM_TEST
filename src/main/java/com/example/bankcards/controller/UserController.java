package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public UserController(AuthenticationManager authManager,
                          UserDetailsService userDetailsService,
                          JwtUtils jwtUtils,
                          PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request){
        try {
            logger.info("Register user with username: {}", request.getUsername());

            // Предполагается, что userDetailsService реализует метод saveUser
            // Если нет — надо сделать кастомный сервис с этим методом
            if (!(userDetailsService instanceof CustomUserDetailsService)) {
                logger.error("userDetailsService does not support user registration");
                return ResponseEntity.status(500).body("Registration service not available");
            }

            ((CustomUserDetailsService) userDetailsService).saveUser(
                    request.getUsername(),
                    passwordEncoder.encode(request.getPassword())
            );

            UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
            String accessToken = jwtUtils.generateAccessToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);

            AuthResponse response = new AuthResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during user registration", e);
            return ResponseEntity.status(500).body("Registration failed");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request){
        try {
            logger.info("Login user with username: {}", request.getUsername());
            authManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            ));

            UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
            String accessToken = jwtUtils.generateAccessToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);

            AuthResponse response = new AuthResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during user login", e);
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            String username = jwtUtils.extractUsername(refreshToken);

            UserDetails user = userDetailsService.loadUserByUsername(username);

            if (jwtUtils.isTokenValid(refreshToken, user)) {
                String newAccessToken = jwtUtils.generateAccessToken(user);
                String newRefreshToken = jwtUtils.generateRefreshToken(user);

                AuthResponse response = new AuthResponse();
                response.setAccessToken(newAccessToken);
                response.setRefreshToken(newRefreshToken);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Invalid refresh token for user: {}", username);
                return ResponseEntity.status(401).body("Invalid refresh token");
            }
        } catch (Exception e) {
            logger.error("Error during token refresh", e);
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
    }
}
