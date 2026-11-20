package com.example.login.repost;


import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RepostRepository extends JpaRepository<Repost, Long> {
    Optional<Repost> findByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT r.postId FROM Repost r where r.postId IN :postIds AND r.userId = :userId")
    Set<Long> findRepostedPostIds(@Param("postIds") Collection<Long> postIds, @Param("userId") Long userId);

    List<Repost> findByUserIdOrderByIdDesc(Long id, Limit limit);

    List<Repost> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Limit limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Repost r" +
            " where r.userId =:userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
