package com.bookhub.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin-seed")
public record AdminSeedProperties(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String role
) {
}
