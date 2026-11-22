package com.example.login.post;

import com.example.login.TestcontainersConfiguration;
import com.example.login.postlike.PostLike;
import com.example.login.user.Users;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Boot 4 패키지: DataJpaTest(data.jpa.test) · AutoConfigureTestDatabase(jdbc.test) · TestEntityManager(jpa.test)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 내장 DB 로 바꾸지 말고 컨테이너 MySQL 을 쓴다
@Import(TestcontainersConfiguration.class) // 우리 설정 클래스 (com.example.login)
class LikeRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    PostRepository postRepository;

    @Test
    void 좋아요_탭_프로젝션에서_reposted가_null이_아니라_false로_나온다() {
        // given - 1. 작성자 & 조회자 저장 (email, passwordHash, nickname, role 이 NOT NULL)
        Users author = Users.builder()
                .email("author@test.com")
                .passwordHash("encoded_pw")
                .nickname("작성자")
                .role("USER")
                .build();
        Users viewer = Users.builder()
                .email("viewer@test.com")
                .passwordHash("encoded_pw")
                .nickname("조회자")
                .role("USER")
                .build();
        em.persist(author);
        em.persist(viewer);

        // given - 2. 게시글 저장 (@Builder.Default 필드는 자동 초기화됨)
        Post post = Post.builder()
                .userId(author.getId())
                .content("테스트 게시글")
                .build();
        em.persist(post);

        // given - 3. viewer가 해당 게시글에 '좋아요'만 누름 (PostLike는 @Builder가 없으므로 생성자 사용)
        PostLike like = new PostLike(post.getId(), viewer.getId());
        em.persist(like);

        em.flush();
        em.clear();

        // when - 좋아요 탭 메서드가 PostLikeRepository에 있다면 해당 메서드로 교체하세요
        List<PostWithLiked> results = postRepository.findAllWithLiked(viewer.getId());

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLiked()).isTrue();
        assertThat(results.get(0).getReposted()).isNotNull();
        assertThat(results.get(0).getReposted()).isFalse();
    }
}
