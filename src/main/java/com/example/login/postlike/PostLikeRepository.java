package com.example.login.postlike;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    List<Long> findByPostLikeIds(Long postId);

    Optional<PostLike> findByIdAndUserId(Long id, Long postId);


}
