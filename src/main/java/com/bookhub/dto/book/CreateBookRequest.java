package com.bookhub.dto.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record CreateBookRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String author,
        @Size(max = 2000) String description,
        @Size(max = 100) String genre,
        @NotNull @Min(1) Integer totalPages,
        MultipartFile pdfFile,
        MultipartFile coverImage
) {
}
