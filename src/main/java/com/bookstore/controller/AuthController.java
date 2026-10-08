package com.bookstore.controller;

import com.bookstore.dto. auth.AuthResponse;
import com.bookstore.dto.auth. LoginRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.bookstore.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authservice;

    public AuthController(AuthService authService) {
        this.authservice = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("hello");
        return ResponseEntity.status(HttpStatus.CREATED).body(authservice.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authservice.login(request));
    }
}