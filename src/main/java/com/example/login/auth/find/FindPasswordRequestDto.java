package com.example.login.auth.find;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindPasswordRequestDto {
    private String email;
    private String name;
}