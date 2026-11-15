package com.example.login.follow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Query("select f.followingId from Follow f where f.followerId =:followerId")
    List<Long> findFollowingIds(@Param("followerId") Long followerId);

    // 나를 팔로우하는 사람들의 ID 목록 (팔로워) -> 비어있던 @Query 완성
    @Query("select f.followerId from Follow f where f.followingId = :followingId")
    List<Long> findFollowerIds(@Param("followingId") Long followingId);


    Long countByFollowerId(Long followerId);

    Long countByFollowingId(Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

}
