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
    @Pattern(regexp="^[a-z0-9_]{4,15}$",
            message="핸들은 영문 소문자, 숫자, _ 4~15자리여야 합니다.")
    private String handle;
    @Size(min=2, max=50, message = "닉네임은 2자 이상, 50자 이하로 입력해주세요.")
    private String nickname;
    @Size(max=255, message = "자기소개는 최대 255자까지 작성할 수 있습니다.")
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
}
