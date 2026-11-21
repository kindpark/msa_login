package com.example.login.feed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class FeedCache {
    private final StringRedisTemplate redis;
    private final Duration ttl;
    public FeedCache(
            StringRedisTemplate redis,
            @Value("${feed.cache-ttl-seconds:60}") long ttlSeconds
    ){
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }
    private static String zKey(Long userId){
        return "feed:" + userId;
    }
    private static String readyKey(Long userId){
        return "feed:ready:" + userId;
    }
    //redis에 key를 가지고 있고 key가 준비되면 boolean의 true를 반환
    // 즉, key를 쓸 준비가 됐다. redis를 쓴다는 뜻인듯
    public boolean isReady(Long userId){
        return Boolean.TRUE.equals(redis.hasKey(readyKey(userId)));
    }
    // 다시 채우는 메서드이고, DB에서 가져온 id들로 ZEST를 새로 만든다.
    public void rebuild(Long userId, Collection<Long> postIdsNewestFirst){
        // zkey 정보 획득
        String zk = zKey(userId);
        // zkey 초기화? 왜 초기화하지?
        // 답 : 이전 내용에는 삭제된 글이나 언팔로우한 사람의 글이 남아 있을 수 있다.
        // 그대로 ZADD만 하면 이것들이 섞이니까, 지우고 새로 넣는 것.
        redis.delete(zk);
        // 새로운 아이디의 컬렉션에 담긴 수 만큼 순회를 한다.
        for(Long id : postIdsNewestFirst) {
            //순회를 돌며 정보를 초기화 한다.
            redis.opsForZSet().add(zk, String.valueOf(id), id.doubleValue());
        }
        // ZSET의 수명을 ttl + 60초로 정하는 것입니다.
        // ready 키(ttl)보다 ZSET이 60초 더 오래 살게 해서,
        // ready가 있는데 ZSET이 먼저 사라지는 일을 막습니다.
        redis.expire(zk, ttl.plusSeconds(60));
        //다 만들었다는 표시임
        redis.opsForValue().set(readyKey(userId), "1", ttl);

    }
    // cursor 보다 작은 아이디를 최신순으로 size개, cursor==null이면 처음부터
    public List<Long> page (Long userId, Long cursor, int size){
        // null일때 무한대. 무한대는 즉 첫번째 커서라는 의미
        double max = cursor == null ? Double.POSITIVE_INFINITY : cursor - 1;
        // 이건 잘 모르겠음
        Set<String> members = redis.opsForZSet()
                .reverseRangeByScore(zKey(userId), Double.NEGATIVE_INFINITY
                        ,max,0,size);
        List<Long> ids = new ArrayList<>();
        if(members != null){
            members.forEach(
                    m -> ids.add(Long.valueOf(m))
            );
        }
        return ids;
    }
    // 접속 종료때 사용할것 같다고 적었는데, 전혀 상관없음.
    // 캐시 무효화 하는 함수임. ready키만 지우면 다음 읽기에 isReady가 false가 되어
    // rebuild가 실행됨. 팔로, 언팔로 직후 호출하는 용도임.
    public void evict(Long userId){
        redis.delete(readyKey(userId));
    }
}
