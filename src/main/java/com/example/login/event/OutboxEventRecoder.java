package com.example.login.event;

import com.example.login.outbox.Outbox;
import com.example.login.outbox.OutboxRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

/**
 * [Outbox 1단계: 기록]
 * 서비스가 eventPublisher.publishEvent(EventMessage) 를 부르면 여기로 온다.
 * 이벤트를 RabbitMQ 로 "바로 보내지 않고" outbox 테이블에 한 줄로 "적어 두기만" 한다.
 * 실제 전송은 OutboxRelayService(2단계) 가 나중에 한다.
 *
 * 전체 흐름
 *   [업무 트랜잭션]  좋아요 저장 → publishEvent → (커밋 직전) 여기서 outbox 행 저장 → 커밋
 *   [발송기, 2초마다] outbox 에서 pending 행을 꺼내 RabbitMQ 로 전송 → sent 로 표시
 */
@Slf4j
@Component
@AllArgsConstructor
public class OutboxEventRecoder {
    // EventMessage 객체 ↔ JSON 문자열 변환기. 스프링이 설정해 둔 것을 주입받아야
    // 날짜(occurredAt) 형식 등이 예전에 RabbitMQ 로 직접 보내던 것과 똑같이 나온다
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    /*
     * BEFORE_COMMIT: 업무 트랜잭션이 "커밋되기 직전", 아직 그 트랜잭션 안에 있을 때 실행된다.
     *  → 좋아요 저장과 outbox 저장이 같은 트랜잭션에 묶인다.
     *  → 커밋되면 둘 다 남고, 롤백되면 둘 다 사라진다 ("좋아요는 됐는데 이벤트는 없음"이 생길 수 없다).
     *
     * 왜 AFTER_COMMIT 이 아닌가:
     *  AFTER_COMMIT 은 이미 커밋이 끝난 뒤라, 여기서 저장이 실패해도 좋아요는 되돌릴 수 없다.
     *  예전 코드(AFTER_COMMIT 에서 RabbitMQ 로 바로 전송)가 바로 그 문제로 이벤트를 잃을 수 있었다.
     *
     * try-catch 가 없는 이유:
     *  저장이나 JSON 변환이 실패하면 예외가 그대로 올라가 업무 트랜잭션 전체가 롤백된다.
     *  예외를 삼키면 좋아요만 커밋되고 이벤트가 사라진다 → Outbox 를 쓰는 의미가 없어진다.
     *  (Jackson 3 의 JacksonException 은 RuntimeException 이라 throws 선언도 필요 없다)
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void connectEvent(EventMessage event) {
            // outbox 테이블에 저장하여 보존 시도
            Outbox save = Outbox.builder()
                    // 이벤트마다 새로 만든 UUID. 같은 이벤트가 두 번 저장되지 않게 unique 컬럼
                    .eventId(event.getEventId())
                    // 라우팅 키로 그대로 쓸 값 (예: "like.created"). 발송기가 꺼내 쓰기 쉽게 따로 둔다
                    .type(event.getType())
                    // 보낼 메시지 "전체"를 JSON 문자열로. 발송기는 이 문자열을 다시 EventMessage 로 읽어 보낸다
                    .payload(objectMapper.writeValueAsString(event))
                    .build();   // 여기까지는 자바 객체일 뿐. 아래 save 를 해야 DB 에 들어간다
            // 업무 트랜잭션 안에서 insert (status = pending)
            outboxRepository.save(save);
            // 참고: 이제 여기서는 "발행"이 아니라 "outbox 저장"만 한다. 실제 발행은 OutboxRelayService
            log.info("rabbitmq 이벤트 발행 성공: type={}", event.getType());
    }
}
