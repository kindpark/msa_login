package com.example.login.integration.x;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.event.EventMessage;
import com.example.login.integration.x.dto.XTimelineResponse;
import com.example.login.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.example.login.integration.x.client.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FakeXClientTest {
    private FakeXClient client;
    @BeforeEach
    void setUp(){
        client = new FakeXClient();
        ReflectionTestUtils.setField(client, "mode", "ok");
        client.init();
    }
    // 커서 없이 10개 요청하면 10개를 주고 다음 커서도 10개를 주는 코드
    @Test
    void loadCursor(){
        XTimelineResponse res = client.fetchTimeline(1L, null, 10);

        //then
        assertThat(res.getItems()).hasSize(10);
        assertThat(res.getItems().get(0).getId()).isEqualTo("post_1");
        assertThat(res.getNextCursor()).isEqualTo("10");
    }
    // 마지막 페이지일때 다음 페이지는 null
    @Test
    void lastPage(){
        // given
        XTimelineResponse res = client.fetchTimeline(1L, "20", 10);

        //then
        assertThat(res.getItems()).hasSize(10);
        assertThat(res.getItems().get(9).getId()).isEqualTo("post_30");
        assertThat(res.getNextCursor()).isNull();

    }
    // 에러모드를 상정
    @Test
    void xIsError(){
        ReflectionTestUtils.setField(client, "mode", "error");

        // when & then
        assertThatThrownBy(() -> client.fetchTimeline(1L, null, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.X_UNAVAILABLE);
    }
    // expired 모드일때 xtokenexpired에러 발생
    @Test
    void xIsErrorinexpired(){
        // given
        ReflectionTestUtils.setField(client, "mode", "expired");

        // when & then
        assertThatThrownBy(() -> client.fetchTimeline(1L, null, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.X_TOKEN_EXPIRED);
    }
    // 자기 자신 팔로우 하면 예외가 터지는가?
    @Test
    void isSelfFollow(){
        Long userId = 1L;
        // when
        assertThatThrownBy(() -> EventMessage.ofFollow(userId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SELF_FOLLOW);
    }
    // 프로필 수정의 merge규칙
    @Test
    void testMerge(){
        // given - 기존 데이터가 채워진 유저 준비 (초기값이 null이 아니어야 유지 여부 검증 가능)
        Users user = Users.builder()
                .nickname("원래닉")
                .bio("원래소개")
                .profileImageUrl("원래사진.jpg")
                .build();

        // when - bio만 새 값으로 넘기고 나머지는 null 전달
        user.updateUsersInfo(null, null, "새소개", null, null);

        // then - 1. 바뀐 필드 검증
        assertThat(user.getBio()).isEqualTo("새소개");

        // then - 2. null을 넘겨 기존 값이 유지되어야 하는 필드 검증
        assertThat(user.getProfileImageUrl()).isEqualTo("원래사진.jpg");
    }
    // 공백 테스트
    @Test
    void isBlankError(){
        Users user = Users.builder()
                .nickname("원래닉")
                .bio("원래소개")
                .profileImageUrl("원래사진.jpg")
                .build();
        // when
        assertThatThrownBy(() -> user.updateUsersInfo("   ", null, null, null, null))
                .isInstanceOf(BusinessException.class)
                        .extracting("errorCode")
                                .isEqualTo(ErrorCode.INVALID_NICKNAME);

    }
}
