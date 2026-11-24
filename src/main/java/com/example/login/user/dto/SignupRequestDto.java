package com.example.login.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequestDto {
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;
    @NotBlank
    @Size(max = 50)
    private String nickname;
    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]{3,14}$")
    private String handle;
}
