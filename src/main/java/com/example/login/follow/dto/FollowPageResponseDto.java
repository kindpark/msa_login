package com.example.login.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FollowPageResponseDto {
    private List<FollowUserResponseDto> list;
    private Long nextCursor;
}
