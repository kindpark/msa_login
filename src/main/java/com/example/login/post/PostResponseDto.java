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
    private Long parentId;
    private String content;
    private boolean liked;
    private boolean reposted;
    private String imageUrl;
    private Integer likeCount;
    private Integer commentCount;
    private Integer repostCount;
    private LocalDateTime createdAt;

    public static PostResponseDto from(
            Post entity,
            boolean liked,
            boolean reposted) {
        return PostResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .parentId(entity.getParentId())
                .content(entity.getContent())
                .liked(liked)
                .reposted(reposted)
                .imageUrl(entity.getImageUrl())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .repostCount(entity.getRepostCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
