package com.bookhub.service;

import com.bookhub.domain.Book;
import com.bookhub.domain.BookRating;
import com.bookhub.domain.User;
import com.bookhub.dto.book.BookDto;
import com.bookhub.dto.book.BookRatingStatsDto;
import com.bookhub.dto.book.CreateBookRequest;
import com.bookhub.dto.book.RateBookRequest;
import com.bookhub.exception.ApiException;
import com.bookhub.mapper.BookMapper;
import com.bookhub.repository.BookRatingRepository;
import com.bookhub.repository.BookRepository;
import com.bookhub.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BookService {
    private static final long MAX_PDF_FILE_SIZE = 25L * 1024 * 1024;
    private static final long MAX_COVER_FILE_SIZE = 5L * 1024 * 1024;

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookRatingRepository bookRatingRepository;
    private final BookMapper bookMapper;
    private final FileStorageService fileStorageService;

    public BookService(BookRepository bookRepository, UserRepository userRepository,
                       BookRatingRepository bookRatingRepository, BookMapper bookMapper,
                       FileStorageService fileStorageService) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.bookRatingRepository = bookRatingRepository;
        this.bookMapper = bookMapper;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<BookDto> getBooks(String search) {
        String term = search == null || search.isBlank() ? null : search.trim();
        return bookRepository.search(term).stream().map(bookMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BookDto getBook(Long id) {
        return bookMapper.toDto(findBook(id));
    }

    @Transactional(readOnly = true)
    public DownloadedFile getPdf(Long id) {
        Book book = findBook(id);
        if (book.getPdfFilePath() == null || book.getPdfFilePath().isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PDF file not found.");
        }
        Path path = fileStorageService.toAbsolutePath(book.getPdfFilePath());
        if (!Files.exists(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PDF file not found.");
        }
        String filename = book.getPdfFileName() == null ? book.getTitle() + ".pdf" : book.getPdfFileName();
        return new DownloadedFile(path, filename);
    }

    @Transactional
    public BookDto create(CreateBookRequest request) {
        validate(request, true);
        OffsetDateTime now = OffsetDateTime.now();
        Book book = new Book();
        fillBook(book, request);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        book.setPdfFilePath(fileStorageService.saveBookFile(request.pdfFile()));
        book.setPdfFileName(request.pdfFile() == null ? null : request.pdfFile().getOriginalFilename());
        book.setCoverImagePath(fileStorageService.saveCover(request.coverImage()));
        return bookMapper.toDto(bookRepository.save(book));
    }

    @Transactional
    public BookDto update(Long id, CreateBookRequest request) {
        validate(request, false);
        Book book = findBook(id);
        fillBook(book, request);
        book.setUpdatedAt(OffsetDateTime.now());
        if (request.pdfFile() != null && !request.pdfFile().isEmpty()) {
            fileStorageService.delete(book.getPdfFilePath());
            book.setPdfFilePath(fileStorageService.saveBookFile(request.pdfFile()));
            book.setPdfFileName(request.pdfFile().getOriginalFilename());
        }
        if (request.coverImage() != null && !request.coverImage().isEmpty()) {
            fileStorageService.delete(book.getCoverImagePath());
            book.setCoverImagePath(fileStorageService.saveCover(request.coverImage()));
        }
        return bookMapper.toDto(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = findBook(id);
        fileStorageService.delete(book.getPdfFilePath());
        fileStorageService.delete(book.getCoverImagePath());
        bookRepository.delete(book);
    }

    @Transactional
    public BookRatingStatsDto rate(Long bookId, RateBookRequest request) {
        Book book = findBook(bookId);
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "User not found."));
        BookRating rating = bookRatingRepository.findByBookIdAndUserId(bookId, request.userId()).orElseGet(BookRating::new);
        rating.setBook(book);
        rating.setUser(user);
        rating.setRatingValue(request.ratingValue());
        if (rating.getCreatedAt() == null) {
            rating.setCreatedAt(OffsetDateTime.now());
        }
        bookRatingRepository.save(rating);
        return ratings(bookId);
    }

    @Transactional(readOnly = true)
    public BookRatingStatsDto ratings(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Book not found.");
        }
        List<BookRating> ratings = bookRatingRepository.findByBookId(bookId);
        if (ratings.isEmpty()) {
            return new BookRatingStatsDto(bookId, BigDecimal.ZERO.setScale(2), 0);
        }
        double average = ratings.stream().mapToInt(BookRating::getRatingValue).average().orElse(0);
        return new BookRatingStatsDto(bookId, BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP), ratings.size());
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Book not found."));
    }

    private void fillBook(Book book, CreateBookRequest request) {
        book.setTitle(request.title().trim());
        book.setAuthor(request.author() == null ? "" : request.author().trim());
        book.setDescription(request.description() == null ? "" : request.description().trim());
        book.setGenre(request.genre() == null || request.genre().isBlank() ? null : request.genre().trim());
        book.setTotalPages(request.totalPages());
    }

    private void validate(CreateBookRequest request, boolean requirePdf) {
        MultipartFile pdf = request.pdfFile();
        MultipartFile cover = request.coverImage();
        if (requirePdf && (pdf == null || pdf.isEmpty())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PDF file is required.");
        }
        if (pdf != null && !pdf.isEmpty()) {
            String name = pdf.getOriginalFilename() == null ? "" : pdf.getOriginalFilename().toLowerCase();
            if (!name.endsWith(".pdf")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "pdfFile must be a .pdf file.");
            }
            if (pdf.getSize() > MAX_PDF_FILE_SIZE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "pdfFile size must be between 1 byte and 25 MB.");
            }
        }
        if (cover != null && cover.getSize() > MAX_COVER_FILE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "coverImage size must not exceed 5 MB.");
        }
    }

    public record DownloadedFile(Path path, String filename) {
    }
}
