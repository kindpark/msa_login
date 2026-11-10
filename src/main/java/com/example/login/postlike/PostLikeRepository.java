package com.example.login.postlike;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    List<Long> findByPostId(Long postId);

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);


}
