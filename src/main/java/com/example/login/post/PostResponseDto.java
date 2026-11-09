package com.example.login.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    private Long id;
    private Long userId;
    private String content;
    private String imageUrl;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;

    public static PostResponseDto from(Post entity) {
        return PostResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .content(entity.getContent())
                .imageUrl(entity.getImageUrl())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
