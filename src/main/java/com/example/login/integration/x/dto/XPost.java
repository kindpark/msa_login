package com.example.login.integration.x.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class XPost {
    String id;
    String text;
    LocalDateTime createdAt;
    XAuthor author;
    String url;
}
