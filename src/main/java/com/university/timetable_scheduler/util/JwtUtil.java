package com.university.timetable_scheduler.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    /** HS256 needs a key of at least 256 bits; a shorter secret is a configuration error. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;

    public record JwtPayload(UUID schoolId) {}

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") Duration expiration) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least %d bytes for HS256; got %d. Generate one with: openssl rand -base64 48"
                            .formatted(MIN_SECRET_BYTES, secretBytes.length));
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiration = expiration;
    }

    public String generateToken(JwtPayload jwtPayload) {
        Instant issueDate = Instant.now();
        return Jwts.builder()
                .claim("schoolId", jwtPayload.schoolId())
                .issuedAt(Date.from(issueDate))
                .expiration(Date.from(issueDate.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public JwtPayload validateToken(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No/Invalid Token");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authToken)
                    .getPayload();
            return new JwtPayload(UUID.fromString(claims.get("schoolId", String.class)));
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired Token");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token");
        }
    }
}
