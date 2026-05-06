package com.bookhub.domain;

import com.bookhub.domain.enums.BotState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "users")
public class User extends BaseEntity {
    @Column(nullable = false, unique = true)
    private Long telegramUserId;

    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(nullable = false, length = 10)
    private String language = "uz";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BotState botState = BotState.NONE;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private List<ReadingProgress> readingProgresses = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<BookRating> bookRatings = new ArrayList<>();
}
