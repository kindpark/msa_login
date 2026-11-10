package com.example.login.postlike;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "post_likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_user",
                columnNames = {"post_id", "user_id"}),
        indexes = @Index(name = "post_id", columnList = "user_id"))
public class PostLike {
    //누가? 어떤글에? 좋아요를 눌렀는가
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long postId;
    private Long userId;
    @CreationTimestamp
    @Column(nullable=false, updatable = false)
    private LocalDateTime likedAt;

    public PostLike(Long postId, Long userId){
        this.postId = postId;
        this.userId = userId;
    }
}
