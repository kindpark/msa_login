package com.example.login.postlike;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT l.postId FROM PostLike l WHERE l.postId IN :postIds AND l.userId = :userId")
    Set<Long> findLikedPostIds(@Param("postIds") Collection<Long> postIds, @Param("userId") Long userId);

}
