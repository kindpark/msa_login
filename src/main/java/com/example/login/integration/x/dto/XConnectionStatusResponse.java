package com.example.login.integration.x.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class XConnectionStatusResponse {
    boolean connected;
    String username;
    // official 에서 연결을 시작할 때만 값이 있다. 있으면 프론트가 그 주소(X 로그인)로 이동한다
    String authorizeUrl;
    public XConnectionStatusResponse(boolean connected, String username) {
        this(connected, username, null);
    }
}
