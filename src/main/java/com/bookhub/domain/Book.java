package com.bookhub.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "books")
public class Book extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String author;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 100)
    private String genre;

    @Column(nullable = false)
    private Integer totalPages;

    private String pdfFilePath;

    @Column(length = 255)
    private String pdfFileName;

    private String coverImagePath;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReadingProgress> readingProgresses = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookRating> bookRatings = new ArrayList<>();
}
