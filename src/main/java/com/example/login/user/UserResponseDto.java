package com.example.login.user;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDto {
    private Long id;
    @Pattern(regexp = "^\\S+$", message = "공백은 포함될 수 없습니다.")
    private String nickname;
    private String profileImageUrl;
    private String bio;

    public static UserResponseDto from(Users users) {
        return UserResponseDto.builder()
                .id(users.getId())
                .nickname(users.getNickname())          // 유지: nickname 매핑
                .profileImageUrl(users.getProfileImageUrl()) // 수정: 프로필 이미지 매핑
                .bio(users.getBio())                    // 수정: 소개글 매핑
                .build();
    }

}
