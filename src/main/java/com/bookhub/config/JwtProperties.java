package com.bookhub.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotBlank
        @Size(min = 32, message = "JWT_SECRET must be at least 32 characters for HS256")
        String secret,
        @Min(1) long expiryMinutes
) {
}
