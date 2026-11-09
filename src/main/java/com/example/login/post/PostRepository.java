package com.example.login.post;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);
    /*
    // 힌트 반영(최신 생성순 정렬 예시): 필요시 서비스 레이어에서 교체하여 사용 가능
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
    */
}

