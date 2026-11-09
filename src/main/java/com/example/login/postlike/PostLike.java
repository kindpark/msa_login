package com.example.login.postlike;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "postlike",
        uniqueConstraints = @UniqueConstraint(name = "uk_writer_likeUser",
                columnNames = {"writer_id", "like_user_id"}),
        indexes = @Index(name = "writer_id", columnList = "like_user_id"))
public class PostLike {
    //누가? 어떤글에? 좋아요를 눌렀는가
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long writerId;
    private Long likeUserId;
    @CreationTimestamp
    @Column(nullable=false, updatable = false)
    private LocalDateTime likedAt;

    public PostLike(Long writerId, Long likeUserId){
        this.writerId = writerId;
        this.likeUserId = likeUserId;
    }
}
