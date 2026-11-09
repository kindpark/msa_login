package com.example.login.post;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Valid
@NotBlank //빈글 저장 못하게
@Builder
public class PostRequestDto {
    private String content;
    private String imageUrl;
}
