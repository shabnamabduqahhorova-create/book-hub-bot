package com.bookhub.mapper;

import com.bookhub.domain.Book;
import com.bookhub.dto.book.BookDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {
    public BookDto toDto(Book book) {
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getGenre(),
                book.getTotalPages(),
                book.getCoverImagePath(),
                "/api/books/%d/download".formatted(book.getId()),
                averageRating(book),
                book.getPdfFileName()
        );
    }

    private BigDecimal averageRating(Book book) {
        if (book.getBookRatings() == null || book.getBookRatings().isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        double average = book.getBookRatings().stream().mapToInt(r -> r.getRatingValue()).average().orElse(0);
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }
}
