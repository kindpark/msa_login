package com.example.login.postlike;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class PostLikeResponseDto {
    private Long writerId;
    private Long likeUserId;

}
