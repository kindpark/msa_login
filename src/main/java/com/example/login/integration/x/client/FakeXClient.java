package com.example.login.integration.x.client;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.integration.x.dto.XAuthor;
import com.example.login.integration.x.dto.XPost;
import com.example.login.integration.x.dto.XTimelineResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Component
@ConditionalOnProperty(name = "integration.x.client", havingValue = "fake", matchIfMissing = true)
public class FakeXClient implements XTimelineClient {

    @Value("${integration.x.fake.mode:ok}")
    private String mode;

    private final List<XPost> mockPosts = new ArrayList<>();
    // fake 타임라인 들고오는 코드
    @PostConstruct
    public void init() {
        LocalDateTime baseTime = LocalDateTime.now();
        for (int i = 1; i <= 30; i++) {
            mockPosts.add(new XPost(
                    "post_" + i,
                    "테스트",
                    baseTime,
                    new XAuthor(
                            "test" + i,
                            "test" + i,
                            "가짜유저",
                            null
                    ),
                    "https://x.com/fake_user/status/post_"+i
            ));
        }
    }
    // 타임라인을 들고오는 코드
    @Override
    public XTimelineResponse fetchTimeline(Long userId, String cursor, int size) {
        switch (mode.toLowerCase()) {
            case "slow" -> {
                try {
                    Thread.sleep(5000); // 5초 대기 (서비스의 3초 타임아웃 유발)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            case "error" -> throw new BusinessException(ErrorCode.X_UNAVAILABLE);
            case "expired" -> throw new BusinessException(ErrorCode.X_TOKEN_EXPIRED);
            case "ok" -> { /* 정상 진행 */ }
            default -> throw new BusinessException(ErrorCode.X_UNAVAILABLE);
        }

        int startIndex = parseCursor(cursor);
        if (startIndex >= mockPosts.size()) {
            return new XTimelineResponse(List.of(), null);
        }

        int endIndex = Math.min(startIndex + size, mockPosts.size());
        List<XPost> sliced = mockPosts.subList(startIndex, endIndex);
        String nextCursor = (endIndex < mockPosts.size()) ? String.valueOf(endIndex) : null;

        return new XTimelineResponse(sliced, nextCursor);
    }
    // 커서 나누는 코드
    private int parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(cursor));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}