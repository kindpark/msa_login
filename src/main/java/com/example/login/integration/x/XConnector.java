package com.example.login.integration.x;

import com.example.login.integration.x.dto.XConnectionStatusResponse;

/**
 * X 연결 방법의 공통 모양. 구현이 둘이고 설정(integration.x.client)에 따라 하나만 빈이 된다.
 *  - FakeXConnector     : 바로 연결된 것으로 저장 (비용 0, 기본값)
 *  - OfficialXConnector : 실제 X 로그인
 * 컨트롤러는 이 인터페이스만 알고 어느 쪽인지 모른다 -> 설정만 바꿔 교체할 수 있다.
 *
 * 부르는 방향: 컨트롤러 -> XConnector -> XOAuthService -> Redis. 거꾸로 부르면 순환이 된다.
 */
public interface XConnector {
    // cookie: x_oauth 쿠키에 넣을 값(browserToken). 쿠키가 필요 없으면(fake, 이미 연결됨) null
    public record ConnectResult(
        XConnectionStatusResponse response,
        String cookie
    ){}

    ConnectResult connect(Long userId);

    // X 에서 돌아왔을 때. 성공/실패만 알려 주고, 어디로 이동할지는 컨트롤러가 정한다
    boolean complete(String code, String state, String browserToken);
}
