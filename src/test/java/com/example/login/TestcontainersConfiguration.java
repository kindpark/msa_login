package com.example.login;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트용 컨테이너. @Import 한 테스트가 뜰 때 Docker 로 잠깐 띄우고, 끝나면 지운다.
 * @ServiceConnection 이 각 컨테이너의 주소·계정·비밀번호를 스프링 설정에 자동으로 넣어 주므로
 * 내 PC 의 user_db · RabbitMQ · Redis 는 건드리지 않는다.
 *
 * - @DataJpaTest 는 DataSource 만 쓰므로 MySQL 만 쓴다 (Rabbit·Redis 빈은 만들어지지만 연결되지 않는다)
 * - @SpringBootTest 는 앱 전체를 띄우므로 셋 다 필요하다 (탈퇴 이벤트 → Rabbit → 리스너 → Redis 캐시 정리)
 *
 * 이미지는 버전을 고정한다 (latest 는 어느 날 바뀌어 테스트가 이유 없이 깨질 수 있다).
 * Testcontainers 2.x: 클래스는 org.testcontainers.mysql / .rabbitmq 패키지 (1.x 예제의 org.testcontainers.containers 아님)
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.0"));
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"));
    }

    /** Redis 는 전용 모듈이 없어 GenericContainer 로 띄운다. name = "redis" 로 어떤 서비스인지 알려 준다 */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.4")).withExposedPorts(6379);
    }
}
