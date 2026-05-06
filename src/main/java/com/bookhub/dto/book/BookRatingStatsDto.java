package com.bookhub.dto.book;

import java.math.BigDecimal;

public record BookRatingStatsDto(Long bookId, BigDecimal averageRating, long totalRatings) {
}
