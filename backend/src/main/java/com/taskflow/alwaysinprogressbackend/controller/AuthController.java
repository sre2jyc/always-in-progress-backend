package com.taskflow.alwaysinprogressbackend.controller;

import com.taskflow.alwaysinprogressbackend.dto.LoginRequest;
import com.taskflow.alwaysinprogressbackend.dto.RegisterRequest;
import com.taskflow.alwaysinprogressbackend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // REGISTER
    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // LOGIN
    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}