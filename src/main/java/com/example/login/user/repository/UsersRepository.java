package com.example.login.user.repository;


import com.example.login.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);

    boolean existsByHandle(String handle);

    Optional<Users> findByHandle(String handle);
}
