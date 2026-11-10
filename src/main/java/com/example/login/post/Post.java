package com.example.login.post;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name="posts")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "update posts set is_deleted=true where id = ?")
@SQLRestriction("is_deleted = false")
@EntityListeners(AuditingEntityListener.class)
public class Post {
    //글 그자체의 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //글 작성자
    private Long userId;
    // 답글 만들기 위한것(없으면 null)
    private Long parentId;
    // 이 게시글 형 대화가 시작된 원본 글의 id
    private Long rootId;

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

    // 삭제 상태 필드 기본값은 false
    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;

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
