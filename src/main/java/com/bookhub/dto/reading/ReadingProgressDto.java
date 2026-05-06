package com.bookhub.dto.reading;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ReadingProgressDto(
        Long id,
        Long userId,
        String userName,
        Long bookId,
        String bookTitle,
        Integer currentPage,
        BigDecimal progressPercent,
        String note,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
