package com.example.login.profile;

import com.example.login.user.Users;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ProfileTapResponseDto {
    private List<Users> users;
    private Long nextCursor;
}
