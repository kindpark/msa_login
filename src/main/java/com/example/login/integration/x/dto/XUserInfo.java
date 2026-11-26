package com.example.login.integration.x.dto;

public record XUserInfo( UserData data
) {
    public record UserData(String id,
                       String name,
                       String username){}
}
