package com.example.login.user;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);
    //Optional<Users> 사용 이유
    //Optional을 사용하면 UserService의 login 메서드에서 유저가 존재하지 않을 때 orElseThrow(...)를 이용해 깔끔하게 예외를 던질 수 있습니다.
    //이메일, 이름 다 쓰는거
    Optional<Users> findByEmailAndName(String email, String name);

    //이메일 기반 유저 정보 탐색
    Optional<Users> findByEmail(String email);


    //이름 기반 유저 정보 탐색
    Optional<Users> findByName(String name);
}
