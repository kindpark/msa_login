package com.example.login.integration.x.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "external_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_account_user_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider; // 예: "X"

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(nullable = true, length = 30)
    private String externalUserId;

    @Builder
    public ExternalAccount(Long userId, String provider, String username,
                           String accessToken, String refreshToken, LocalDateTime tokenExpiresAt,
                           String externalUserId) {
        this.userId = userId;
        this.provider = provider;
        this.username = username;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.externalUserId = externalUserId;
    }

    public static ExternalAccount createFakeX(Long userId) {
        return ExternalAccount.builder()
                .userId(userId)
                .provider("X")
                .username("fake_user")
                .build();
    }

    public static ExternalAccount createX(Long userId,
                        String username,
                        String externalUserId,
                        String accessToken,
                        String refreshToken,
                        LocalDateTime tokenExpiresAt){
        return ExternalAccount.builder()
                .userId(userId)
                .provider("X")
                .username(username)
                .externalUserId(externalUserId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(tokenExpiresAt)
                .build();
    }
    public void setTokens(String accessToken, String refreshToken, LocalDateTime tokenExpiresAt){
        // 토큰이 없으면, 기본값을 넣어줘야함
        // 기본값을 넣는것을 따로 처리만 하고 response null 처리를 한다.
        if(refreshToken == null){
            this.accessToken = accessToken;
            this.tokenExpiresAt = tokenExpiresAt;
            return;
        }
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
    }
}