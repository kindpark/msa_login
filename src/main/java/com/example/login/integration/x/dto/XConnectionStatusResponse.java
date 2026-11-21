package com.example.login.integration.x.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class XConnectionStatusResponse {
    boolean connected;
    String username;
}
