package com.example.login.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    @Column(name="password_hash")
    private String passwordHash;
    private String nickname;
    private LocalDateTime createAt;

    // 엔티티는 외부 기술(PasswordEncoder 등)을 몰라야 함.
    // 비즈니스 메서드는 순수한 값만 인자로 받아서 변경함.
    public void changePassword(String newEncodedPassword) {
        this.passwordHash = newEncodedPassword;
    }
}
