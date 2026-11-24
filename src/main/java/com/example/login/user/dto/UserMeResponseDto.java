package com.example.login.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserMeResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String role;
}
