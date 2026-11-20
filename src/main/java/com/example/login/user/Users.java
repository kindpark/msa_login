package com.example.login.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SQLRestriction("deleted_at is null")
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false, length = 50)
    private String nickname;
    @Column(nullable = false, length = 20)
    private String role;

    // 기획서에서 profile_image_url VARCHAR(500), bio VARCHAR(255), 둘 다 NULL 허용
    @Column(length = 500)
    private String profileImageUrl;
    @Column(length = 255)
    private String bio;
    @Column(length= 15, unique = true)
    private String handle;
    @Column(length = 500)
    private String bannerImageUrl;
    // 저장할 때 Hibernate 가 현재 시각을 채우고, 이후 수정되지 않게 한다.
    // nullable = false 는 붙이지 않는다.
    // 이미 있는 계정 행에는 값이 없어 ddl-auto=update 의 컬럼 추가가 실패한다.
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(updatable = true)
    private LocalDateTime updatedAt;

    @Column(updatable = true)
    private LocalDateTime deletedAt;

    // 엔티티는 외부 기술(PasswordEncoder 등)을 몰라야 함.
    // 비즈니스 메서드는 순수한 값만 인자로 받아서 변경함.
    public void changePassword(String newEncodedPassword) {
        this.passwordHash = newEncodedPassword;
    }
    public void updateUsersInfo(
            String nickname,
            @Size(max=500)
            String profileImageUrl,
            String bio,
            String handle,
            @Size(max=500)
            String bannerImageUrl
    ){
        this.nickname = merge(nickname, this.nickname);
        this.bio = merge(bio, this.bio);
        this.handle = merge(handle, this.handle);
        this.profileImageUrl = merge(profileImageUrl, this.profileImageUrl);
        this.bannerImageUrl = merge(bannerImageUrl, this.bannerImageUrl);
        updatedAt = LocalDateTime.now();
    }
    private String merge(String a, String b){
        if(a == null){
            return b;
        }
        if(a.isBlank()) {
            return null;
        }
        return a;
    }

    public void withdraw(){
        this.role = "deleted";

        this.handle = null;
        this.deletedAt = LocalDateTime.now();
        this.email = "deleted_" + this.id + "_" + this.email;
    }
}
