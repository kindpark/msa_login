package com.example.login.integration.x.client;

import com.example.login.integration.x.dto.XTimelineResponse;

public interface XTimelineClient {
    XTimelineResponse fetchTimeline(Long userId, String cursor, int size);
}
