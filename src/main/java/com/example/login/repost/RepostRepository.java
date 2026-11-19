package com.example.login.repost;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RepostRepository extends JpaRepository<Repost, Long> {
    Optional<Repost> findByUserIdAndPostId(Long userId, Long postId);

}
