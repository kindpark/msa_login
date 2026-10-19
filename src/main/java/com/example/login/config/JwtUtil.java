package com.example.login.config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
        private final String SECRET_KEY = "";
        private final long EXPIRATION_TIME = 1000L * 60 * 60 * 2;
        private SecretKey getSigningKey(){
            return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        }

        //사용자 이메일 주입받아 JWT ACCESS TOKEN 생성
    public String generateToken(String email){
            Date now = new Date();
            Date validity = new Date(now.getTime() + EXPIRATION_TIME);

            return Jwts.builder()
                    .subject(email)
                    .issuedAt(now)
                    .expiration(now)
                    .signWith(getSigningKey())
                    .compact();
    }
    /**
     * 토큰에서 사용자 이메일(Subject) 추출
     */
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
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
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
