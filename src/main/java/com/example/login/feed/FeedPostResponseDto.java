package com.example.login.feed;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
//post, 이게 필요하지 않을수도 있을듯. postResponseDto와 역할이 같은것 같음
public class FeedPostResponseDto {
    private Long id;
    private Long userId;
    private Long parentId;
    private String content;
    private String imageUrl;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean liked;
    private LocalDateTime createdAt;
}
