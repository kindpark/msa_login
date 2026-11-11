package com.example.login.feed;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.ValueGenerationType;

@Entity
@Getter
@Table(name="feed")
public class Feed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(updatable = false) //정보가 없으면 null을 줘야하나?
    private Long postId;

    @Column(nullable = false)
    private Long ownerId;

    private String type;

    private Long actorId;
}
