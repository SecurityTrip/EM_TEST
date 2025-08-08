package com.example.bankcards.service;

import com.example.bankcards.dto.AuthResponse;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RefreshRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authManager;

    public UserService(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            AuthenticationManager authManager
    ) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authManager = authManager;
    }

    public AuthResponse register(RegisterRequest request) {
        try {
            if (!(userDetailsService instanceof CustomUserDetailsService)) {
                logger.error("userDetailsService does not support user registration");
                return null;
            }
            if (request.getRole().equals(User.Role.USER)){
                ((CustomUserDetailsService) userDetailsService).saveUser(
                        request.getUsername(),
                        passwordEncoder.encode(request.getPassword()),
                        request.getRole());
            }
            if (request.getRole().equals(User.Role.ADMIN)){
                ((CustomUserDetailsService) userDetailsService).saveUser(
                        request.getUsername(),
                        passwordEncoder.encode(request.getPassword()),
                        request.getRole()
                );
            }

            UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());

            String accessToken = jwtUtils.generateAccessToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);

            AuthResponse response = new AuthResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);

            return response;

        } catch (Exception e) {
            logger.error("Error during registration: ", e);
            return null;
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());

            String accessToken = jwtUtils.generateAccessToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);

            AuthResponse response = new AuthResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);

            return response;

        } catch (Exception e) {
            logger.error("Error during login: ", e);
            return null;
        }
    }

    public AuthResponse refresh(RefreshRequest request) {
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

                return response;
            } else {
                throw new Exception("Invalid refresh token");
            }

        } catch (Exception e) {
            logger.error("Error during token refresh: ", e);
            return null;
        }
    }
}