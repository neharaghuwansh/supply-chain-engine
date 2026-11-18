package com.supplychain.engine.controller;

import com.supplychain.engine.dto.LoginRequest;
import com.supplychain.engine.dto.LoginResponse;
import com.supplychain.engine.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}