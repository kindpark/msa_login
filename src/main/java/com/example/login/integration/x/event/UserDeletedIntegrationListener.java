package com.example.login.integration.x.event;

import com.example.login.config.RabbitMQConfig;
import com.example.login.event.EventMessage;
import com.example.login.integration.x.repository.ExternalAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedIntegrationListener {
    private final ExternalAccountRepository externalAccountRepository;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.INTEGRATION_USER_DELETED_QUEUE)
    @Transactional
    public void onUserDeleted(EventMessage event) {
        Long userId = event.getActorId();
        if (userId == null) {
            log.warn("user.deleted 메시지에 actorId가 없습니다: eventId={}", event.getEventId());
            return;
        }

        log.info("RabbitMQ user.deleted 수신 - 외부 연동 계정 및 캐시 삭제 시작: userId={}, eventId={}",
                userId, event.getEventId());

        // DB에 저장된 외부 계정(X 토큰 포함) 일괄 삭제
        externalAccountRepository.deleteAllByUserId(userId);

        // Redis 타임라인 캐시 정리
        evict(userId);

        log.info("RabbitMQ user.deleted 처리 완료: userId={}", userId);
    }
    // redis 타임라인 정리하여 갱신하는 코드
    private void evict(Long userId) {
        String pattern = String.format("x:timeline:%d:*", userId);
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        Set<String> keysToDelete = new HashSet<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
            }
        } catch (Exception e) {
            log.warn("X 타임라인 캐시 스캔 실패 (TTL 만료로 정리됨): userId={}", userId, e);
            return;
        }

        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }
    }