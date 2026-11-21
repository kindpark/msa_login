package com.example.login.feed;

import com.example.login.post.PostResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class FeedPageResponseDto {
    private List<PostResponseDto> posts;
    private Long nextCursor;

}
