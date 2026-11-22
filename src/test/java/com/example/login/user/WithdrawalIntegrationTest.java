package com.example.login.user;

import com.example.login.TestcontainersConfiguration;
import com.example.login.integration.x.entity.ExternalAccount;
import com.example.login.integration.x.repository.ExternalAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 탈퇴 → user.deleted (커밋 후 발행) → RabbitMQ → integration 리스너 → X 연동 행 삭제 를 끝까지 확인한다.
 *
 * - @DataJpaTest 가 아니라 @SpringBootTest: 서비스 · 이벤트 발행 · RabbitTemplate · @RabbitListener 까지 앱 전체가 필요하다
 * - 테스트에 @Transactional 을 붙이지 않는다: 붙이면 끝날 때 롤백되어 커밋이 없고,
 *   커밋 후(AFTER_COMMIT)에 나가는 user.deleted 이벤트도 나가지 않는다
 * - 리스너는 다른 스레드에서 메시지를 받으므로 결과를 바로 보지 않고 Awaitility 로 기다린다
 *   (Testcontainers 안의 shaded awaitility 가 아니라 org.awaitility 를 쓴다)
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TestcontainersConfiguration.class)
class WithdrawalIntegrationTest {

    @Autowired
    UsersService usersService;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    ExternalAccountRepository externalAccountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void 탈퇴하면_X_연동_행이_이벤트로_지워진다() {
        // given - 비밀번호는 인코딩해서 저장해야 deleteAccount 의 비밀번호 확인을 통과한다
        String rawPassword = "password123!";
        Users user = usersRepository.save(Users.builder()
                .email("withdraw@test.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .nickname("탈퇴유저")
                .role("USER")
                .build());
        Long userId = user.getId();

        // given - 지워질 대상: X 연동 행
        externalAccountRepository.save(ExternalAccount.createFakeX(userId));
        assertThat(externalAccountRepository.findByUserIdAndProvider(userId, "X")).isPresent();

        // DeleteAccountRequestDto 는 JSON 으로만 채워지는 DTO(setter·생성자 없음)라 테스트에서는 필드에 직접 넣는다
        DeleteAccountRequestDto request = new DeleteAccountRequestDto();
        ReflectionTestUtils.setField(request, "password", rawPassword);

        // when - 실제 탈퇴 경로. 이 메서드의 트랜잭션이 커밋된 뒤 user.deleted 가 RabbitMQ 로 나간다
        usersService.deleteAccount(request, userId);

        // then 1 - 사용자는 바로 탈퇴 상태 (@SQLRestriction("deleted_at is null") 때문에 더는 조회되지 않는다)
        assertThat(usersRepository.findById(userId)).isEmpty();

        // then 2 - X 연동 행은 Rabbit 을 거쳐 비동기로 지워지므로 최대 10초 기다린다
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(externalAccountRepository.findByUserIdAndProvider(userId, "X")).isEmpty());
    }
}
