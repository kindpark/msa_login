package com.example.login.feed;

import com.example.login.post.PostResponseDto;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedService {
    //private final FeedRepository feedRepository;


    @Async //임시
    public RedisTemplate<FeedPostResponseDto> getFeedList(Long userId, Long cursor, PostResponseDto dto, int size) {

    }



}
