package com.example.login.integration.x.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record XTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") long expiresIn
) {}
