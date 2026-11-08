package com.example.login.follow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponseDto {
    private boolean following;
}
