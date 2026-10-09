package com.bookstore.dto.auth;

import lombok.Builder;

@Builder
public record AuthResponse(
String token,
String tokenType,
String username,
String role
) {
    public static AuthResponse bearer(String token, String username, String role) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(username)
                .role(role)
                .build();
    }
}

