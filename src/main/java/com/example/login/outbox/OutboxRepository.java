package com.example.login.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    // 메서드 이름으로 쿼리를 만든다 (파생 쿼리):
    //   find Top100 By Status OrderBy Id Asc
    //   → select ... from outbox where status = ? order by id asc limit 100
    // 이름이 틀리면 서버가 뜰 때 바로 에러가 나서 알 수 있다 (네이티브 쿼리는 실행할 때까지 검사하지 않음).
    // @Param 은 @Query 의 :이름 과 연결할 때 쓰는 것이라 파생 쿼리에는 없어도 된다 (위치 순서로 들어간다).
    // 서버가 여러 대가 되면 같은 행을 두 대가 동시에 보내지 않게
    // @Lock(PESSIMISTIC_WRITE) + SKIP LOCKED 를 트랜잭션 안에서 쓰는 방식을 검토한다 (API_SPEC §9.3).
    List<Outbox> findTop100ByStatusOrderByIdAsc(@Param("status") String status);

    Optional<Outbox> findByEventId(String eventId);
}

