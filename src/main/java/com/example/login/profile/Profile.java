package com.example.login.profile;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String handle;
    private String nickname;
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
    private LocalDateTime createdAt;
    private Integer followerCount;
    private Integer followingCount;
    private Long postCount;
    private Boolean me;
    private Boolean followedByMe;


}
