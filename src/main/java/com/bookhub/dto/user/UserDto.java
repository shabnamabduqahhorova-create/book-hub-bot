package com.bookhub.dto.user;

import java.time.OffsetDateTime;

public record UserDto(
        Long id,
        Long telegramUserId,
        String username,
        String firstName,
        String lastName,
        OffsetDateTime createdAt
) {
}
