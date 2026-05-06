package com.bookhub.controller;

import com.bookhub.dto.book.BookDto;
import com.bookhub.dto.book.BookRatingStatsDto;
import com.bookhub.dto.book.RateBookRequest;
import com.bookhub.service.BookService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BooksController {
    private final BookService bookService;

    public BooksController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookDto> getAll(@RequestParam(required = false) String search) {
        return bookService.getBooks(search);
    }

    @GetMapping("/{id}")
    public BookDto getById(@PathVariable Long id) {
        return bookService.getBook(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id) {
        BookService.DownloadedFile file = bookService.getPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(new FileSystemResource(file.path()));
    }

    @PostMapping("/{bookId}/rating")
    public BookRatingStatsDto rate(@PathVariable Long bookId, @Valid @RequestBody RateBookRequest request) {
        return bookService.rate(bookId, request);
    }

    @GetMapping("/{bookId}/ratings")
    public BookRatingStatsDto ratings(@PathVariable Long bookId) {
        return bookService.ratings(bookId);
    }
}
