package com.example.login.event;

import com.example.login.config.RabbitMQConfig;
import com.example.login.outbox.Outbox;
import com.example.login.outbox.OutboxRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * [Outbox 2단계: 발송기(relay)]
 * outbox 테이블에서 아직 안 보낸(pending) 행을 꺼내 RabbitMQ 로 보내고, 보낸 행은 sent 로 표시한다.
 * 누가 이벤트를 건네주는 게 아니라, 스스로 2초마다 DB 를 보고 일을 찾는다.
 *
 * 전달 보장: "최소 한 번" (at-least-once)
 *  - RabbitMQ 로 보낸 직후, sent 표시를 저장하기 전에 서버가 죽으면 그 행은 pending 으로 남아
 *    다음에 한 번 더 보내진다. 그래서 받는 쪽은 같은 이벤트가 두 번 와도 괜찮아야 한다
 *    (알림 서비스: event_id unique / X·탈퇴 정리: 두 번 지워도 결과가 같음).
 */
@Slf4j
@Service
// @Scheduled 를 동작하게 하는 스위치. 이게 없으면 @Scheduled 가 붙어 있어도 아무 일도 일어나지 않는다
// (앱 전체 설정이라 보통은 메인 클래스나 @Configuration 에 둔다. 여기 있어도 동작은 같다)
@EnableScheduling
@AllArgsConstructor
public class OutboxRelayService {
    // 이 횟수만큼 실패하면 failed 로 두고 더는 보내지 않는다 (무한 재시도 방지, API_SPEC §9.3)
    private static final int MAX_ATTEMPTS = 10;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // 반복문 전체에 트랜젝션을 걸면, 중간의 행에 에러가 났을때 앞의 행들도
    // 롤백이 되어 다시 전송되기에
    // 매 반복문마다 트랜젝션을 줄수 없는 노릇이라..
    //  → 보충: 이 메서드에는 @Transactional 이 없지만, 아래 outboxRepository.save(...) 한 번 한 번이
    //    각자 자기 트랜잭션으로 바로 커밋된다 (JpaRepository 의 save 는 기본으로 @Transactional).
    //    그래서 "행마다 따로 커밋"이 이미 되고 있다. 50번째 행이 실패해도 1~49번째의 sent 표시는 남는다.
    //
    // fixedDelay = 2000: "이전 실행이 끝나고 나서" 2초 뒤에 다시 실행한다.
    //  (fixedRate 는 시작 시각 기준이라, 한 번이 오래 걸리면 다음 실행과 겹칠 수 있다)
    //  메서드에 파라미터가 있으면 스케줄러가 부를 수 없으므로 파라미터 없이 둔다.
    @Scheduled(fixedDelay = 2000)
    public void processPendingEvents() {
        // 1) 안 보낸 행을 오래된 순(id 오름차순)으로 최대 100개 꺼낸다.
        //    메서드 이름이 곧 쿼리: where status = ? order by id asc limit 100
        //    이 조회에 맞춰 엔티티에 (status, id) 인덱스를 걸어 두었다.
        //    한 번에 100개로 자르는 이유: 밀린 행이 많아도 한 주기가 너무 길어지지 않게. 남은 건 다음 주기에
        List<Outbox> pendingEvents = outboxRepository.findTop100ByStatusOrderByIdAsc("pending");
        for (Outbox outbox : pendingEvents) {
            // 2) 행마다 try-catch: 한 행이 실패해도 반복문이 멈추지 않고 다음 행을 계속 보낸다
            try {
                // 3) 저장해 둔 JSON 문자열(payload)을 다시 EventMessage 객체로 읽는다.
                //    문자열을 그대로 convertAndSend 에 넣으면 JSON 변환기가 문자열을 한 번 더 감싸
                //    "\"{...}\"" 처럼 나가고, 받는 쪽이 EventMessage 로 읽지 못한다. 그래서 객체로 되돌려 보낸다
                EventMessage message = objectMapper.readValue(outbox.getPayload(), EventMessage.class);
                // 설정 파일의 Exchange 이름
                String exchange = RabbitMQConfig.EXCHANGE_NAME;
                // 4) RabbitMQ 로 전송. 라우팅 키 = 이벤트 type (예: "user.deleted")
                //    → exchange 가 이 키에 바인딩된 큐들(알림 큐, X 탈퇴 정리 큐)에 한 통씩 넣는다.
                //    참고: 예외 없이 끝나면 "보냈다"이지 "브로커가 받았다"는 확인은 아니다
                //    (확실히 하려면 Publisher Confirms. 지금은 Rabbit 이 꺼져 있으면 여기서 예외가 나므로 충분)
                rabbitTemplate.convertAndSend(exchange, outbox.getType(), message);

                // 5) 보냈으면 sent 로 바꾸고 보낸 시각을 기록 → 저장(이 save 가 바로 커밋된다)
                //    다음 주기의 조회(status = pending)에는 더 이상 안 잡힌다 → 다시 보내지 않는다
                outbox.markAsSent();
                outboxRepository.save(outbox);
            } catch (Exception e) {
                // 6) 실패: JSON 읽기 실패, RabbitMQ 연결 실패 등.
                //    상태는 pending 그대로 두고 시도 횟수만 올린다 → 다음 주기에 다시 시도된다
                outbox.increaseAttempts();

                if (outbox.getAttempts() >= MAX_ATTEMPTS) {
                    // 7) 10번 실패하면 failed 로 바꿔 더는 꺼내지 않는다(조회 조건이 pending 이므로).
                    //    사람이 로그와 행을 보고 원인을 고친 뒤 status 를 pending 으로 되돌리면 다시 보내진다
                    outbox.markAsFailed();
                    log.error("아웃박스 이벤트 발행 최종 실패 - id: {}, type: {}, error: {}",
                            outbox.getId(), outbox.getType(), e.getMessage(), e);
                } else {
                    log.warn("아웃박스 이벤트 발행 일시 실패 ({}/{}) - id: {}, error: {}",
                            outbox.getAttempts(), MAX_ATTEMPTS, outbox.getId(), e.getMessage());
                }
                // 실패 상태/시도 횟수도 독립적으로 즉시 커밋 (만약 여기서 DB 장애가 나면 다음 주기에 재시도됨)
                // 8) 이 save 까지 실패하면(DB 장애) 로그만 남기고 다음 행으로 넘어간다.
                //    DB 에는 이전 상태(pending, 이전 시도 횟수)가 그대로 있으므로 다음 주기에 다시 시도된다
                try {
                    outboxRepository.save(outbox);
                } catch (Exception dbEx) {
                    log.error("아웃박스 실패 상태 업데이트 중 DB 오류 발생 - id: {}", outbox.getId(), dbEx);
                }
            }
        }
    }
}
