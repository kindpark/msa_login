package com.example.login.outbox;

import com.example.login.TestcontainersConfiguration;
import com.example.login.event.EventMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TestcontainersConfiguration.class)
public class OutboxTransactionTest {
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("트랜잭션이 롤백되면 발행한 이벤트의 outbox 행도 저장되지 않는다")
    void rollback_doesNotSaveOutbox() {
        // given
        EventMessage event = EventMessage.ofFollow(1L, 2L);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly(); // 트랜잭션 강제 롤백
        });

        // then
        Optional<Outbox> found = outboxRepository.findByEventId(event.getEventId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("짝 테스트: 트랜잭션이 정상 커밋되면 outbox 행이 저장된다")
    void commit_savesOutbox() {
        // given
        EventMessage event = EventMessage.ofFollow(1L, 2L);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            // setRollbackOnly() 없음 -> 정상 커밋됨
        });

        // then
        Optional<Outbox> found = outboxRepository.findByEventId(event.getEventId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("pending");
    }
}
