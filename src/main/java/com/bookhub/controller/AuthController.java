package com.bookhub.controller;

import com.bookhub.dto.auth.AdminLoginRequest;
import com.bookhub.dto.auth.AuthResponse;
import com.bookhub.dto.auth.RegisterAdminRequest;
import com.bookhub.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterAdminRequest request) {
        authService.register(request);
        return ResponseEntity.ok(Map.of("message", "Admin registered successfully."));
    }
}
