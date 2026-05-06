package com.bookhub.service;

import com.bookhub.domain.AdminUser;
import com.bookhub.dto.auth.AdminLoginRequest;
import com.bookhub.dto.auth.AuthResponse;
import com.bookhub.dto.auth.RegisterAdminRequest;
import com.bookhub.exception.ApiException;
import com.bookhub.repository.AdminUserRepository;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AdminLoginRequest request) {
        AdminUser admin = adminUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        return new AuthResponse(jwtService.generate(admin), admin.getUsername(), admin.getRole());
    }

    @Transactional
    public void register(RegisterAdminRequest request) {
        if (adminUserRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username already exists.");
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(request.username().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(request.normalizedRole());
        admin.setCreatedAt(OffsetDateTime.now());
        adminUserRepository.save(admin);
    }
}
