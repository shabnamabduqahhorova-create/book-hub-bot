package com.bookhub.service;

import com.bookhub.domain.enums.ReadingStatus;
import com.bookhub.dto.dashboard.BookMetricDto;
import com.bookhub.dto.dashboard.DashboardStatisticsDto;
import com.bookhub.repository.BookRatingRepository;
import com.bookhub.repository.BookRepository;
import com.bookhub.repository.ReadingProgressRepository;
import com.bookhub.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final BookRatingRepository bookRatingRepository;

    public DashboardService(BookRepository bookRepository, UserRepository userRepository,
                            ReadingProgressRepository readingProgressRepository, BookRatingRepository bookRatingRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.readingProgressRepository = readingProgressRepository;
        this.bookRatingRepository = bookRatingRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatisticsDto statistics() {
        return new DashboardStatisticsDto(
                bookRepository.count(),
                userRepository.count(),
                readingProgressRepository.countByStatus(ReadingStatus.READING),
                readingProgressRepository.countByStatus(ReadingStatus.COMPLETED),
                mostReadBooks(),
                highestRatedBooks()
        );
    }

    @Transactional(readOnly = true)
    public List<BookMetricDto> mostReadBooks() {
        return readingProgressRepository.mostReadBooks().stream().map(this::metric).toList();
    }

    @Transactional(readOnly = true)
    public List<BookMetricDto> highestRatedBooks() {
        return bookRatingRepository.highestRatedBooks().stream().map(this::metric).toList();
    }

    private BookMetricDto metric(Object[] row) {
        Long bookId = ((Number) row[0]).longValue();
        String title = (String) row[1];
        long count = ((Number) row[2]).longValue();
        BigDecimal score = toBigDecimal(row[3]).setScale(2, RoundingMode.HALF_UP);
        return new BookMetricDto(bookId, title, count, score);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
