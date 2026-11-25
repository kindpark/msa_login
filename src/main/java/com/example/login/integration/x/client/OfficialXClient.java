package com.example.login.integration.x.client;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.integration.x.dto.XTimelineResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
public class OfficialXClient implements XTimelineClient{
    @Override
    public XTimelineResponse fetchTimeline(Long userId, String cursor, int size){
        throw new BusinessException(ErrorCode.X_UNAVAILABLE);
    }
}
