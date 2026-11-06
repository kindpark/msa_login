package com.example.login.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenResponse {
    private String token;
    private String tokenType;

    public TokenResponse(String token, String tokenType){
        this.token = token;
        this.tokenType = tokenType;
    }
}
