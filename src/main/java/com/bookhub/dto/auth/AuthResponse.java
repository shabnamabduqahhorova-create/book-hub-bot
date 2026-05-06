package com.bookhub.dto.auth;

public record AuthResponse(String token, String username, String role) {
}
