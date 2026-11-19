package com.example.login.repost;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "reposts",
        uniqueConstraints = @UniqueConstraint(name = "uk_repost_user_post",
                columnNames = {"post_id", "user_id"}),
        indexes = @Index(name = "idx_repost_user_id", columnList = "user_id"))
public class Repost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long postId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    Repost(Long userId, Long postId){
        this.userId = userId;
        this.postId = postId;
    }
}
