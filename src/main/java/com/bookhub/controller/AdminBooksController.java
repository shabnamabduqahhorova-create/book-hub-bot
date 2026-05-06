package com.bookhub.controller;

import com.bookhub.dto.book.BookDto;
import com.bookhub.dto.book.CreateBookRequest;
import com.bookhub.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/books")
public class AdminBooksController {
    private final BookService bookService;

    public AdminBooksController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public BookDto create(@Valid @ModelAttribute CreateBookRequest request) {
        return bookService.create(request);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public BookDto update(@PathVariable Long id, @Valid @ModelAttribute CreateBookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
