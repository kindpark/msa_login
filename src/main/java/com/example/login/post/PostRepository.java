package com.example.login.post;


import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    //회원 탈퇴할때 글을 지우는 처리
    List<Post> findByUserId(Long userId);

    Long countByUserId(Long userId);

    // 프로필 "게시물 N개": 답글(parentId 있음)은 빼고 원글만 센다. 삭제된 글은 @SQLRestriction 이 걸러낸다.
    Long countByUserIdAndParentIdIsNull(Long userId);

    List<Post> findAllById(Iterable<Long> ids);
    //최근 포스트 한 사람들중 포스트아이디,

    @Query("select p.id from Post p" +
            " where p.userId in :authorIds" +
            " and p.parentId is null order by p.id desc")
    List<Long> findRecentPostIdsByAuthors(
            @Param("authorIds") List<Long> authorIds,
            Limit size);
    // 글마다 나의 좋아요가 있는가?
    // DB의 조건문에 대해선 내가 안배웠으므로..
    String TEXT_ON_MY_LIKE = "select p as post," +
            " case when l.id is not null then true else false end as liked"+
            " from Post p" +
            " left join PostLike l" +
            " on l.postId = p.id" +
            " and l.userId =:viewerId";
    // 내가 좋아요를 했는지에 대한 여부
    // 나에 대한 식별
    @Query(TEXT_ON_MY_LIKE + " where p.id =:id")
    Optional<PostWithLiked> findByIdWithLiked(
            @Param("id") Long id,
            @Param("viewerId") Long viewerId
    );
    // 답글 목록 및 답글에 좋아요를 눌렀는가
    @Query(TEXT_ON_MY_LIKE +
            " where p.parentId =:parentId" +
            " and (:cursor is null or p.id > :cursor)" +
            " order by p.id asc")
    List<PostWithLiked> findThreadWithLiked(
        @Param("parentId") Long parentId,
        @Param("cursor") Long cursor,
        @Param("viewerId") Long viewerId,
        Limit limit
    );
    // userId 기반 Liked를 찾음
    @Query(TEXT_ON_MY_LIKE +
        " where p.userId =:userId" +
            " order by p.id desc")
    List<PostWithLiked> findByUserIdWithLiked(
            @Param("userId") Long userId,
            @Param("viewerId") Long viewerId
    );
    // 모든 글과 내가 좋아요를 눌렀는가
    @Query(TEXT_ON_MY_LIKE +
    " order by p.id desc")
    List<PostWithLiked> findAllWithLiked(
            @Param("viewerId") Long viewerId);
    // 커서 기반 쓰레드 페이지 조회, 쓰레드 기반 배치 작업
    @Query("select p from Post p" +
            " where p.parentId = :parentId" +
            " and ( :cursor is null or p.id > :cursor)" +
            " order by p.id asc")
    List<Post> findThread(
            @Param("parentId") Long parentId,
            @Param("cursor") Long cursor,
            Limit limit);
    // 댓글 계산
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount+1 where p.id= :id")
    void increaseComment(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount-1 "
    + "where p.id= :id and p.commentCount > 0")
    void decreaseComment(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount+1" +
            " where p.id= :id")
    void increaseLike(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount-1 "
            + "where p.id= :id and p.likeCount > 0")
    void decreaseLike(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p" +
            " set p.repostCount = p.repostCount+1" +
            " where p.id=:id")
    void increaseRepostCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p" +
            " set p.repostCount = p.repostCount-1" +
            " where p.id=:id" +
            " and p.repostCount > 0")
    void decreaseRepostCount(@Param("id") Long id);
//커서별로 유저 구분
    // 답글이 아닌 첫글
    @Query(TEXT_ON_MY_LIKE+
            " where p.userId = :userId" +
            " and p.parentId is null" +
            " and (:cursor is null or p.id < :cursor)" +
            " order by p.id desc")
    List<PostWithLiked> findUserRootPostsWithCursor(
            @Param("viewerId") Long viewerId,
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Limit pageable
    );

    @Query(TEXT_ON_MY_LIKE+
            " where p.userId = :userId" +
            " and p.parentId is not null" +
            " and (:cursor is null or p.id < :cursor)" +
            " order by p.id desc")
    List<PostWithLiked> findUserRepliesPostsWithCursor(
            @Param("viewerId") Long viewerId,
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Limit pageable
    );
    @Query(TEXT_ON_MY_LIKE+
            " where p.userId = :userId" +
            " and p.imageUrl is not null" +
            " and (:cursor is null or p.id < :cursor)" +
            " order by p.id desc")
    List<PostWithLiked> findUserMediaPostsWithCursor(
            @Param("viewerId") Long viewerId,
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Limit pageable
    );
    // 리포스트 카운트 리턴
    @Query("select p.repostCount" +
            " from Post p" +
            " where p.id =:id")
    Integer findRepostCountById(Long postId);
}

