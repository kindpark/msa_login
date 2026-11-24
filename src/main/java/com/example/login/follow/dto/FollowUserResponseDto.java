package com.example.login.follow.dto;

import com.example.login.user.entity.Users;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowUserResponseDto {
    private Long id;
    private String handle;
    private String nickname;
    private String bio;
    private String profileImageUrl;
    private Boolean followedByMe;

    public static FollowUserResponseDto from(Users user, Boolean followedByMe){
        return FollowUserResponseDto.builder()
                .id(user.getId())
                .handle(user.getHandle())
                .nickname(user.getNickname())
                .bio(user.getBio())
                .profileImageUrl(user.getProfileImageUrl())
                .followedByMe(followedByMe)
                .build();
    }
}
