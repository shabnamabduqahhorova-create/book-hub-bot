package com.bookhub.dto.book;

import java.math.BigDecimal;

public record BookDto(
        Long id,
        String title,
        String author,
        String description,
        String genre,
        Integer totalPages,
        String coverImageUrl,
        String pdfDownloadUrl,
        BigDecimal averageRating,
        String pdfFileName
) {
}
