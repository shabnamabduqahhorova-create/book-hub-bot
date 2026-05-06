package com.bookhub.dto.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RateBookRequest(
        @NotNull Long userId,
        @Min(1) @Max(5) Integer ratingValue
) {
}
