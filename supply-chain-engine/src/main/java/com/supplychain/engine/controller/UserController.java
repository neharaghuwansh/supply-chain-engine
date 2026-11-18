package com.supplychain.engine.controller;

import com.supplychain.engine.dto.UserRequest;
import com.supplychain.engine.dto.UserResponse;
import com.supplychain.engine.entity.User;
import com.supplychain.engine.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public UserResponse createCashier(
            @RequestBody UserRequest request) {

        User user = userService.createCashier(request);

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.isActive()
        );
    }
}