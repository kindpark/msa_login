package com.example.login.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "sns.events";
    // Excharge 빈. C# 부분과 완벽히 같아야함
    @Bean
    public TopicExchange snsEventExcharge(){
        // C#쪽 초안보고 짜는중인데 이것과 동일해야함
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // JSON 메시지 변환. 이게 없으면 Rabbit 템플릿이 자바 직렬화로 객체를 보내기에 필수
    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper){
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
