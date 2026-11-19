package com.example.login.repost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RepostResponseDto {
    private boolean reposted;
    private Integer repostCount;
    public static RepostResponseDto from(boolean reposted,
                                         Integer repostCount){
        return RepostResponseDto.builder()
                .reposted(reposted)
                .repostCount(repostCount)
                .build();
    }
}
