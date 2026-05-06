package com.bookhub.dto.reading;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReadingProgressRequest(
        @NotNull Long userId,
        @NotNull Long bookId,
        @NotNull @Min(1) Integer currentPage,
        @Size(max = 1000) String note
) {
}
