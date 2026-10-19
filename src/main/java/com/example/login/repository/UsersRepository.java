package com.example.login.repository;


import com.example.login.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);
    //Optional<Users> 사용 이유
    //Optional을 사용하면 UserService의 login 메서드에서 유저가 존재하지 않을 때 orElseThrow(...)를 이용해 깔끔하게 예외를 던질 수 있습니다.
    Optional<Users> findByEmail(String email);
}
