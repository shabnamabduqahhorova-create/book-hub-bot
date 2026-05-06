package com.bookhub.mapper;

import com.bookhub.domain.ReadingProgress;
import com.bookhub.domain.User;
import com.bookhub.dto.user.UserDto;
import com.bookhub.dto.user.UserReadingProgressDto;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getTelegramUserId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt()
        );
    }

    public UserReadingProgressDto toReadingProgressDto(ReadingProgress progress) {
        return new UserReadingProgressDto(
                progress.getId(),
                progress.getBook().getId(),
                progress.getBook().getTitle(),
                progress.getCurrentPage(),
                progress.getProgressPercent(),
                progress.getNote(),
                toPascal(progress.getStatus().name())
        );
    }

    public String displayName(User user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return ((user.getFirstName() == null ? "" : user.getFirstName()) + " " +
                (user.getLastName() == null ? "" : user.getLastName())).trim();
    }

    public String toPascal(String enumName) {
        String lower = enumName.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
