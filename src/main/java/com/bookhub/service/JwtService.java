package com.bookhub.service;

import com.bookhub.config.JwtProperties;
import com.bookhub.domain.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(AdminUser adminUser) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .subject(adminUser.getUsername())
                .claim("adminId", adminUser.getId())
                .claim("role", adminUser.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.expiryMinutes() * 60)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
