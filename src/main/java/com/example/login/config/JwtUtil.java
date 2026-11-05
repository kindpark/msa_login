package com.example.login.config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
        private final long EXPIRATION_TIME = 1000L * 60 * 60 * 2;

    @Value("${jwt.secret}")
    private String secretKeyString;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // 프로퍼티 값이 null이거나 32바이트 미만인 경우 안전하게 fallback 키로 보완
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            String fallbackKey = "default-secret-key-must-be-at-least-32-bytes-long!";
            keyBytes = fallbackKey.getBytes(StandardCharsets.UTF_8);
        }
        // 싱글톤 SecretKey 객체 생성
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }
        //사용자 이메일 주입받아 JWT ACCESS TOKEN 생성
    public String generateToken(String email){
            Date now = new Date();
            Date validity = new Date(now.getTime() + EXPIRATION_TIME);

            return Jwts.builder()
                    .subject(email)
                    .issuedAt(now)
                    .expiration(now)
                    .signWith(secretKey)
                    .compact();
    }
    /**
     * 토큰에서 사용자 이메일(Subject) 추출
     */
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * 토큰 유효성 및 만료 여부 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
