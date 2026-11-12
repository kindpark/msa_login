package com.example.login.feed;
/*
아직 쓰이지 않는 코드
import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpHeaders;
import java.util.List;

@Component
public class UserClient {
    private final RestClient client;
    // 초기화용 생성자
    public UserClient(
            @Value("${user-service.url}")
            String baseUrl
    ){
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }
    // 팔로잉 id 수집(이것이 팔로우 한것들만 구분해서 피드에 수집하는 역할을 하도록 하는 구분자일듯)
    // 각종 예외를 throw 하는 메서드이기도 한듯
    public List<Long> followingIds(String authorizationHeader) {
        try{
            //팔로잉 목록 수집, 다만 following의 getFollow가 아직 완전히 구현되지 않음
            var req = client.get().uri("/api/v1/follows/followings")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            // TraceIdFilter라는것 주입
            String traceId = MDC.get(TraceIdFilter.MDC_KEY);

            if(traceId != null){
                req = req.header(TraceIdFilter.HEADER, traceId);
            }
            List<Long> ids = req.retrieve().body(new ParameterizedTypeReference<List<Long>>() {
            });
            return ids == null ? List.of() : ids;
        }catch(RestClientException e){
            throw new BusinessException(ErrorCode.UPSTREAM_UNVAILABLE);
        }
    }
}
*/