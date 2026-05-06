package com.bookhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram")
public record TelegramBotProperties(
        boolean enabled,
        String botToken,
        String apiUrl
) {
    public boolean hasToken() {
        return botToken != null && !botToken.isBlank();
    }
}
