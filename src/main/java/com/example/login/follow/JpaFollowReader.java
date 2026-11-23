package com.example.login.follow;

import com.example.login.feed.FollowReader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FollowReader 의 단일 앱용 구현. 같은 DB 의 follows 테이블을 직접 읽는다.
 * import 방향: follow -> feed (인터페이스). feed 는 이 클래스를 모른다.
 * 서비스를 나누면 이 클래스는 user-service 로 따라가고,
 * post-feed-service 에는 HTTP 로 호출하는 구현체가 새로 생긴다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name="feed.follow-reader",
        havingValue = "jpa", matchIfMissing = true)
public class JpaFollowReader implements FollowReader {

    private final FollowRepository followRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Long> followingIds(Long userId) {
        return followRepository.findFollowingIds(userId);
    }
}
