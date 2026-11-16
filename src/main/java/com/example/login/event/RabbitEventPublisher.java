package com.example.login.event;

import com.example.login.config.RabbitMQConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@AllArgsConstructor
public class RabbitEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void connectEvent(EventMessage event){
        try{
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    event.getType(),
                    event
            );
            log.info("rabbitmq 이벤트 발행 성공: type={}", event.getType());
        }
        catch (Exception e){
            log.error("rabbitmq 이벤트 발행 실패: type={}, payload={}", event.getType(), event.getEventId(), e);
        }
    }
}
