package com.example.login.event;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.post.UserDeletedListener;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class EventMessage {

    private static final String TYPE_FOLLOW_CREATED = "follow.created";
    private static final String TYPE_POSTLIKE_CREATED = "like.created";
    private static final String TYPE_REPLY_CREATED = "reply.created";
    private static final String TYPE_REPOST_CREATED = "repost.created";
    private static final String TYPE_QUOTE_CREATED = "quote.created";
    private static final String TYPE_USER_DELETED = "user.deleted";
    private final String eventId;
    private final String type;
    private final Instant occurredAt;
    private final Long actorId;
    private final Long recipientId;
    private final Long postId;
    private final Long parentId;

    private EventMessage(
            String eventId,
            String type,
            Instant occurredAt,
            Long actorId,
            Long recipientId,
            Long postId,
            Long parentId
    ){
        this.eventId = eventId;
        this.type = type;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.recipientId = recipientId;
        this.postId = postId;
        this.parentId = parentId;
    }
    public static EventMessage ofFollow(Long userId, Long targetUserId) {
        if(userId.equals(targetUserId)){
            throw new BusinessException(ErrorCode.SELF_FOLLOW);
        }
        return new EventMessage(
                UUID.randomUUID().toString(), // 새 UUID를 문자열로 변환
                TYPE_FOLLOW_CREATED,          // "follow.created"
                Instant.now(),                // 현재 UTC 시각 (ISO-8601 직렬화용)
                userId,                       // actorId (팔로우를 건 사람)
                targetUserId,                 // recipientId (팔로우를 받은 사람)
                null,                         // postId
                null                          // parentId
        );
    }

    public static EventMessage ofPostLike(
            Long likerId,
            Long postId,
            Long postAuthorId) {
        return new EventMessage(
                UUID.randomUUID().toString(),
                TYPE_POSTLIKE_CREATED,
                Instant.now(),
                likerId,
                postAuthorId,
                postId,
                null
        );
    }

    public static EventMessage ofReply(
            Long userId,
            Long parentWriterId,
            Long newReplyId,
            Long parentId
    ){
        return new EventMessage(
                UUID.randomUUID().toString(),
                TYPE_REPLY_CREATED,
                Instant.now(),
                userId,
                parentWriterId,
                newReplyId,
                parentId
        );
    }

    public static EventMessage ofRepost(
            Long userId,
            Long postId,
            Long postAuthorId
    ) {
        return new EventMessage(
                UUID.randomUUID().toString(),
                TYPE_REPOST_CREATED,
                Instant.now(),
                userId,
                postAuthorId,
                postId,
                null
        );
    }
    public static EventMessage ofQuote(
            Long userId,
            Long recipientId,
            Long postId
            ) {
        return new EventMessage(
                UUID.randomUUID().toString(),
                TYPE_QUOTE_CREATED,
                Instant.now(),
                userId,
                recipientId,
                postId,
                null
        );
    }
    public static EventMessage ofUserDeleted(
            Long userId
    ){

        return new EventMessage(
                UUID.randomUUID().toString(),
                TYPE_USER_DELETED,
                Instant.now(),
                userId,
                null,
                null,
                null
        );
    }
}
