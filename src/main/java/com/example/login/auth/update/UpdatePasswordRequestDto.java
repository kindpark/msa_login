package com.example.login.auth.update;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePasswordRequestDto {
    @NotBlank
    private String currentPassword;
    // 회원가입과 같은 규칙
    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
