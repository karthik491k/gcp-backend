package com.logancee.globalcart.service;

import com.logancee.globalcart.dto.AuthRequest;
import com.logancee.globalcart.dto.AuthResponse;
import com.logancee.globalcart.model.Role;
import com.logancee.globalcart.model.User;
import com.logancee.globalcart.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class AuthService {

    private final SpannerDataService spannerDataService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(SpannerDataService spannerDataService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager) {
        this.spannerDataService = spannerDataService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse login(AuthRequest.LoginRequest req) {
        User user = spannerDataService.findUserByEmail(req.getEmail())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (Exception ex) {
            // Demo fallback logging
        }

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .user(user)
                .build();
    }

    public AuthResponse register(AuthRequest.RegisterRequest req) {
        if (spannerDataService.findUserByEmail(req.getEmail()).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "User already exists with email: " + req.getEmail());
        }

        // Public registration MUST always assign CUSTOMER role
        User newUser = User.builder()
                .id("user-" + UUID.randomUUID().toString().substring(0, 8))
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .role(Role.CUSTOMER)
                .avatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80")
                .createdAt(new Date().toString())
                .build();

        spannerDataService.saveUser(newUser);

        String token = tokenProvider.generateToken(newUser.getEmail(), newUser.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .user(newUser)
                .build();
    }
}
