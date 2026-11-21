package com.example.login.integration.x.repository;

import com.example.login.integration.x.entity.ExternalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, Long> {

    Optional<ExternalAccount> findByUserIdAndProvider(Long userId, String provider);

    void deleteByUserIdAndProvider(Long userId, String provider);
}
