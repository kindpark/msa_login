package com.example.login.integration.x;

import com.example.login.integration.x.dto.XConnectionStatusResponse;

public interface XConnector {
    public record ConnectResult(
        XConnectionStatusResponse response,
        String cookie
    ){};

    ConnectResult connect(Long userId);
}
