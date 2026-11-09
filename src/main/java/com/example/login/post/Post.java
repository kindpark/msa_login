package com.example.login.post;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name="posts")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String content;

    private String imageUrl;
    @Builder.Default
    @Column(nullable=false)
    private Integer likeCount=0;
    @Builder.Default
    @Column(nullable = false)
    private Integer commentCount=0;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Post(Long userId,
                String content,
                String imageUrl){
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public void update(Long userId, String content, String imageUrl){
        if(!userId.equals(this.userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        this.content = content;
        this.imageUrl = imageUrl;
    }
}
