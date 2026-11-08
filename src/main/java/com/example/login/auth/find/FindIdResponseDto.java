package com.example.login.auth.find;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FindIdResponseDto {
    private List<String> emails;
    //private String nickname;
}