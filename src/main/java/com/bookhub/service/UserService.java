package com.bookhub.service;

import com.bookhub.dto.user.UserDto;
import com.bookhub.dto.user.UserReadingProgressDto;
import com.bookhub.mapper.UserMapper;
import com.bookhub.repository.ReadingProgressRepository;
import com.bookhub.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, ReadingProgressRepository readingProgressRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.readingProgressRepository = readingProgressRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(userMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UserReadingProgressDto> getReadingProgress(Long userId) {
        return readingProgressRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(userMapper::toReadingProgressDto)
                .toList();
    }
}
