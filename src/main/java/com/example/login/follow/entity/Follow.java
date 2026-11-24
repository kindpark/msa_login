package com.example.login.follow.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "follows",
        uniqueConstraints = @UniqueConstraint(name = "uk_follower_following",
                columnNames = {"follower_id", "following_id"}),
        indexes = @Index(name = "idx_following_id", columnList = "following_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false)
    private Long followingId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Follow(Long followerId, Long followingId){
        this.followerId = followerId;
        this.followingId = followingId;
    }
}
