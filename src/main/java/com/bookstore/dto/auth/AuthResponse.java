package com.bookstore.dto.auth;

public record AuthResponse(
String token,
String tokenType,
String username,
String role
) {
    public static AuthResponse bearer(String token, String username, String role) {
        return new AuthResponse(token, "Bearer", username, role);
    }
}


