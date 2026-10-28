// com/example/login/dto/UpdatePasswordRequestDto.java
package com.example.login.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePasswordRequestDto {
    private String currentPassword;
    private String newPassword;
}