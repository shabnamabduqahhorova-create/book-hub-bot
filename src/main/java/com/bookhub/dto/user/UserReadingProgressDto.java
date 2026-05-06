package com.bookhub.dto.user;

import java.math.BigDecimal;

public record UserReadingProgressDto(
        Long readingProgressId,
        Long bookId,
        String bookTitle,
        Integer currentPage,
        BigDecimal progressPercent,
        String note,
        String status
) {
}
