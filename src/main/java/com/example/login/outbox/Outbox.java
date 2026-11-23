package com.example.login.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * outbox 테이블의 한 행 = "보내야 할 이벤트 하나".
 * 업무 트랜잭션 안에서 pending 으로 저장되고(OutboxEventRecoder),
 * 발송기(OutboxRelayService)가 RabbitMQ 로 보낸 뒤 sent 로 바꾼다. 10번 실패하면 failed.
 *
 *   pending ──보냄──▶ sent
 *      │
 *      └─10번 실패──▶ failed (사람이 확인)
 */
@Entity
@Table(
        name = "outbox",
        indexes = {
                // 발송기 조회(where status = ? order by id)에 맞춘 인덱스. status 로 먼저 좁히고 id 순서대로 읽는다
                @Index(name = "idx_status_id", columnList = "status, id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EventMessage 의 UUID. unique 라서 같은 이벤트가 outbox 에 두 번 들어가지 않는다 (UUID 문자열 = 36자)
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    // 이벤트 종류 = RabbitMQ 라우팅 키 (예: "like.created")
    @Column(nullable = false, length = 100)
    private String type;

    // 보낼 메시지 전체(EventMessage)를 JSON 문자열로. MySQL JSON 타입은 저장할 때 키 순서·공백을 정리하지만
    // 받는 쪽은 키 이름으로 읽으므로 상관없다
    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;

    // "pending" / "sent" / "failed". MySQL ENUM 대신 문자열 컬럼이라 상태를 추가해도 테이블을 고칠 필요가 없다
    @Column(nullable = false, length = 20)
    private String status;

    // 보내기에 실패한 횟수. MAX_ATTEMPTS(10) 에 닿으면 failed
    @Column(nullable = false)
    private int attempts;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 보낸 시각 (sent / failed 가 된 시각). pending 동안은 null.
    // created_at 과의 차이가 "이벤트가 얼마나 늦게 나갔는지"다 (발송기 주기 2초 + 처리 시간)
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder
    public Outbox(String eventId, String type, String payload) {
        this.eventId = eventId;
        this.type = type;
        this.payload = payload;
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
        this.attempts = 0;
    }

    // 상태 변경 편의 메서드 (선택)
    public void markAsSent() {
        this.status = "sent";
        this.sentAt = LocalDateTime.now();
    }
    // 상태 변경 편의 메서드 (선택)
    public void markAsFailed() {
        this.status = "failed";
        this.sentAt = LocalDateTime.now();
    }

    public void increaseAttempts() {
        this.attempts++;
    }
}
