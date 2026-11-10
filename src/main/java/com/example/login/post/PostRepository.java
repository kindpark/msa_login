package com.example.login.post;


import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);

    // 커서 기반 쓰레드 페이지 조회
    @Query("select p from Post p" +
            " where p.parentId = :parentId" +
            " and ( :cursor is null or p.id > :cursor)" +
            " order by p.id asc")
    List<Post> findThread(
            @Param("parentId") Long parentId,
            @Param("cursor") Long cursor,
            Limit limit);
    // 댓글 계산
    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount+1 where p.id= :id")
    void increaseComment(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount-1 "
    + "where p.id= :id and p.commentCount > 0")
    void decreaseComment(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount+1" +
            " where p.id= :id")
    void increaseLike(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount-1 "
            + "where p.id= :id and p.likeCount > 0")
    void decreaseLike(@Param("id") Long id);
}

