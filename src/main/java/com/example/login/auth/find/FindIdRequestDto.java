package com.example.login.auth.find;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindIdRequestDto {
    @NotBlank
    @Size(max = 50)
    private String nickname;
}
