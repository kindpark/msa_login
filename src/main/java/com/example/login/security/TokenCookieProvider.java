package com.example.login.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class TokenCookieProvider {
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;
    public static final String TOKEN_COOKIE = "user_token";
    public ResponseCookie create(String token) {
        return build(token, 2 * 60 * 60);
    }
    public ResponseCookie expire(){
        return build("", 0);
    }

    private ResponseCookie build(String value, long maxAge) {
        return ResponseCookie.from(TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }
}