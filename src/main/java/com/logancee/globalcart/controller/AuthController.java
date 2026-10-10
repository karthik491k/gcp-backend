package com.logancee.globalcart.controller;

import com.logancee.globalcart.dto.AuthRequest;
import com.logancee.globalcart.dto.AuthResponse;
import com.logancee.globalcart.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest.RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody AuthRequest.ForgotPasswordRequest req) {
        return ResponseEntity.ok(Map.of("message", "[DEV SIMULATION] Password reset request processed for " + req.getEmail() + ". In production, an email with a secure token will be dispatched."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody AuthRequest.ResetPasswordRequest req) {
        return ResponseEntity.ok(Map.of("message", "Password has been successfully updated."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
