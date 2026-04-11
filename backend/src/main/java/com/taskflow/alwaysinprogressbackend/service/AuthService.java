package com.taskflow.alwaysinprogressbackend.service;

import com.taskflow.alwaysinprogressbackend.dto.LoginRequest;
import com.taskflow.alwaysinprogressbackend.dto.RegisterRequest;
import com.taskflow.alwaysinprogressbackend.model.User;
import com.taskflow.alwaysinprogressbackend.repository.UserRepository;
import com.taskflow.alwaysinprogressbackend.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // REGISTER
    public Map<String, String> register(RegisterRequest request) {

        // Optional: check if user exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("USER_ALREADY_EXISTS");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        log.info("User registered | userId={} email={}", user.getId(), user.getEmail());

        return Map.of(
                "message", "User registered successfully",
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "name", user.getName()
        );
    }

    // LOGIN
    public Map<String, String> login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("INVALID_CREDENTIALS"));

        boolean isPasswordValid = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!isPasswordValid) {
            throw new RuntimeException("INVALID_CREDENTIALS");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        log.info("User login successful | userId={} email={}", user.getId(), user.getEmail());

        return Map.of(
                "token", token,
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "name", user.getName()
        );
    }
}