package com.example.login.follow.repository;

import com.example.login.follow.entity.Follow;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Query("select f.followingId from Follow f where f.followerId =:followerId")
    List<Long> findFollowingIds(@Param("followerId") Long followerId);

    // 나를 팔로우하는 사람들의 ID 목록 (팔로워) -> 비어있던 @Query 완성
    @Query("select f.followerId from Follow f where f.followingId = :followingId")
    List<Long> findFollowerIds(@Param("followingId") Long followingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Follow f" +
            " where f.followerId =:userId or f.followingId =:userId")
    void deleteAllByUser(@Param("userId") Long userId);

    Long countByFollowerId(Long followerId);

    Long countByFollowingId(Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
// 첫 페이지 조회
    List<Follow> findByFollowingIdOrderByIdDesc(Long followingId, Limit limit);

    // ①-2 다음 페이지 조회 (cursor != null 일 때)
    List<Follow> findByFollowingIdAndIdLessThanOrderByIdDesc(Long followingId, Long cursor, Limit limit);

    // ③ 로그인한 유저(viewerId)가 조회된 팔로워 목록 중 이미 팔로우한 내역 조회
    List<Follow> findByFollowerIdAndFollowingIdIn(Long followerId, Collection<Long> followingIds);

    List<Follow> findByFollowerIdOrderByIdDesc(Long followerId, Limit limit);

    List<Follow> findByFollowerIdAndIdLessThanOrderByIdDesc(Long followerId, Long cursor, Limit limit);
}
