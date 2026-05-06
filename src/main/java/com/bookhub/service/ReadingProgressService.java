package com.bookhub.service;

import com.bookhub.domain.Book;
import com.bookhub.domain.ReadingProgress;
import com.bookhub.domain.User;
import com.bookhub.domain.enums.ReadingStatus;
import com.bookhub.dto.reading.CreateReadingProgressRequest;
import com.bookhub.dto.reading.ReadingProgressDto;
import com.bookhub.dto.reading.UpdateReadingProgressRequest;
import com.bookhub.exception.ApiException;
import com.bookhub.mapper.UserMapper;
import com.bookhub.repository.BookRepository;
import com.bookhub.repository.ReadingProgressRepository;
import com.bookhub.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingProgressService {
    private final ReadingProgressRepository readingProgressRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final UserMapper userMapper;

    public ReadingProgressService(ReadingProgressRepository readingProgressRepository, UserRepository userRepository,
                                  BookRepository bookRepository, UserMapper userMapper) {
        this.readingProgressRepository = readingProgressRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<ReadingProgressDto> getAll() {
        return readingProgressRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingProgressDto> getByUser(Long userId) {
        return readingProgressRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ReadingProgressDto create(CreateReadingProgressRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "User not found."));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Book not found."));
        if (readingProgressRepository.existsByUserIdAndBookId(user.getId(), book.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reading progress already exists for this book.");
        }
        validateCurrentPage(request.currentPage(), book.getTotalPages());
        OffsetDateTime now = OffsetDateTime.now();
        ReadingProgress progress = new ReadingProgress();
        progress.setUser(user);
        progress.setBook(book);
        progress.setCurrentPage(request.currentPage());
        progress.setProgressPercent(calculateProgress(request.currentPage(), book.getTotalPages()));
        progress.setNote(trimToNull(request.note()));
        progress.setStatus(statusFor(request.currentPage(), book.getTotalPages()));
        progress.setCreatedAt(now);
        progress.setUpdatedAt(now);
        return toDto(readingProgressRepository.save(progress));
    }

    @Transactional
    public ReadingProgressDto update(Long id, UpdateReadingProgressRequest request) {
        ReadingProgress progress = readingProgressRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reading progress not found."));
        validateCurrentPage(request.currentPage(), progress.getBook().getTotalPages());
        progress.setCurrentPage(request.currentPage());
        progress.setProgressPercent(calculateProgress(request.currentPage(), progress.getBook().getTotalPages()));
        progress.setNote(trimToNull(request.note()));
        progress.setStatus(statusFor(request.currentPage(), progress.getBook().getTotalPages()));
        progress.setUpdatedAt(OffsetDateTime.now());
        return toDto(progress);
    }

    private ReadingProgressDto toDto(ReadingProgress progress) {
        return new ReadingProgressDto(
                progress.getId(),
                progress.getUser().getId(),
                userMapper.displayName(progress.getUser()),
                progress.getBook().getId(),
                progress.getBook().getTitle(),
                progress.getCurrentPage(),
                progress.getProgressPercent(),
                progress.getNote(),
                userMapper.toPascal(progress.getStatus().name()),
                progress.getCreatedAt(),
                progress.getUpdatedAt()
        );
    }

    private void validateCurrentPage(Integer currentPage, Integer totalPages) {
        if (currentPage == null || currentPage < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current page cannot be less than 1.");
        }
        if (currentPage > totalPages) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current page cannot be greater than total pages.");
        }
    }

    private BigDecimal calculateProgress(int currentPage, int totalPages) {
        return BigDecimal.valueOf(currentPage)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalPages), 2, RoundingMode.HALF_UP);
    }

    private ReadingStatus statusFor(int currentPage, int totalPages) {
        return currentPage == totalPages ? ReadingStatus.COMPLETED : ReadingStatus.READING;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
