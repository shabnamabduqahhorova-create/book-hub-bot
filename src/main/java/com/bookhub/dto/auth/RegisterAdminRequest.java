package com.bookhub.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAdminRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 6, max = 128) String password,
        String role
) {
    public String normalizedRole() {
        return role == null || role.isBlank() ? "Admin" : role.trim();
    }
}
