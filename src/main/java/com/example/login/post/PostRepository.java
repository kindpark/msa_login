package com.example.login.post;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);

    // 필요시 서비스 레이어에서 교체하여 사용 가능
    //List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Post> findByParentIdOrderByCreatedAtAsc(Long userId);
    // 댓글 계산
    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount+1 where p.id= :id")
    void increaseComment(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount-1 "
    + "where p.id= :id and p.commentCount > 0")
    void decreaseComment(@Param("id") Long id);
}

