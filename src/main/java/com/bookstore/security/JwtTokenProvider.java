package com.bookstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey signingkey;
    private final Long expirationMs;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms}") Long expirationMs) {
        this.signingkey = Keys.hmacShaKeyFor((Decoders.BASE64.decode(secret)));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("role", principal.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingkey)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getusername(String token) {
        return parse(token).getSubject();
    }

    public Long getuserId(String token) {
        return parse(token).get("uid", Long.class);
    }

    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingkey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}