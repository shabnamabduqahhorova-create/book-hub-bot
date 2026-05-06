package com.bookhub.domain;

import com.bookhub.domain.enums.ReadingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reading_progresses", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"}))
public class ReadingProgress extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(nullable = false)
    private Integer currentPage;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal progressPercent;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReadingStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
