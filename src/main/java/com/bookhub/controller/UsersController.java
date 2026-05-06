package com.bookhub.controller;

import com.bookhub.dto.user.UserDto;
import com.bookhub.dto.user.UserReadingProgressDto;
import com.bookhub.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> getUsers() {
        return userService.getUsers();
    }

    @GetMapping("/{id}/reading-progress")
    public List<UserReadingProgressDto> getReadingProgress(@PathVariable Long id) {
        return userService.getReadingProgress(id);
    }
}
