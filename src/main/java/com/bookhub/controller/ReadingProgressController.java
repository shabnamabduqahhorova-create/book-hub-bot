package com.bookhub.controller;

import com.bookhub.dto.reading.CreateReadingProgressRequest;
import com.bookhub.dto.reading.ReadingProgressDto;
import com.bookhub.dto.reading.UpdateReadingProgressRequest;
import com.bookhub.service.ReadingProgressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reading-progress")
public class ReadingProgressController {
    private final ReadingProgressService readingProgressService;

    public ReadingProgressController(ReadingProgressService readingProgressService) {
        this.readingProgressService = readingProgressService;
    }

    @GetMapping
    public List<ReadingProgressDto> getAll() {
        return readingProgressService.getAll();
    }

    @GetMapping("/user/{userId}")
    public List<ReadingProgressDto> getByUser(@PathVariable Long userId) {
        return readingProgressService.getByUser(userId);
    }

    @PostMapping
    public ReadingProgressDto create(@Valid @RequestBody CreateReadingProgressRequest request) {
        return readingProgressService.create(request);
    }

    @PutMapping("/{id}")
    public ReadingProgressDto update(@PathVariable Long id, @Valid @RequestBody UpdateReadingProgressRequest request) {
        return readingProgressService.update(id, request);
    }
}
