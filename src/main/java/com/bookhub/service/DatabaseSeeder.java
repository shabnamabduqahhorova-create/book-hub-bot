package com.bookhub.service;

import com.bookhub.config.AdminSeedProperties;
import com.bookhub.domain.AdminUser;
import com.bookhub.domain.Book;
import com.bookhub.repository.AdminUserRepository;
import com.bookhub.repository.BookRepository;
import java.time.OffsetDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseSeeder implements ApplicationRunner {
    private final AdminUserRepository adminUserRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminSeedProperties;

    public DatabaseSeeder(AdminUserRepository adminUserRepository, BookRepository bookRepository,
                          PasswordEncoder passwordEncoder, AdminSeedProperties adminSeedProperties) {
        this.adminUserRepository = adminUserRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminSeedProperties = adminSeedProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminUserRepository.existsByUsername(adminSeedProperties.username())) {
            AdminUser admin = new AdminUser();
            admin.setUsername(adminSeedProperties.username());
            admin.setPasswordHash(passwordEncoder.encode(adminSeedProperties.password()));
            admin.setRole(adminSeedProperties.role());
            admin.setCreatedAt(OffsetDateTime.now());
            adminUserRepository.save(admin);
        }

        if (bookRepository.count() == 0) {
            bookRepository.save(sampleBook("Atomic Habits", "James Clear",
                    "A practical guide to building good habits and breaking bad ones.", 320));
            bookRepository.save(sampleBook("Clean Code", "Robert C. Martin",
                    "A handbook of agile software craftsmanship.", 464));
        }
    }

    private Book sampleBook(String title, String author, String description, int totalPages) {
        OffsetDateTime now = OffsetDateTime.now();
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setTotalPages(totalPages);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        return book;
    }
}
