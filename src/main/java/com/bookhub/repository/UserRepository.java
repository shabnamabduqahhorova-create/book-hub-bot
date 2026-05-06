package com.bookhub.repository;

import com.bookhub.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllByOrderByCreatedAtDesc();

    Optional<User> findByTelegramUserId(Long telegramUserId);
}
