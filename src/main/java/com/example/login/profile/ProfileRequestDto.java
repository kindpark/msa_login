package com.example.login.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ProfileRequestDto {
    private String handle;
    private String nickname;
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
    private LocalDateTime createdAt;
}
