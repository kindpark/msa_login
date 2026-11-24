package com.example.login.profile.dto;

import com.example.login.user.entity.Users;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ProfileTapResponseDto {
    private List<Users> users;
    private Long nextCursor;
}
