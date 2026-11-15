package com.example.login.profile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileRequestDto {
    private String handle;
    private String nickname;
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
}
