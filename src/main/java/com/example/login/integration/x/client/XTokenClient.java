package com.example.login.integration.x.client;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.integration.x.dto.XTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
public class XTokenClient {
    // 로그
    private static final Logger log = LoggerFactory.getLogger(XTokenClient.class);
    private static final String TOKEN_URL = "https://api.x.com/2/oauth2/token";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public XTokenClient(
    @Value("${integration.x.client-id}") String clientId,
    @Value("${integration.x.client-secret}") String clientSecret,
    @Value("${integration.x.redirect-uri}") String redirectUri
    ){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory)
                .build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public XTokenResponse exchangeCode(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("code_verifier", codeVerifier);

        try{
            // POST로 시작. 토큰을 교환하기 때문이다.
            XTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret)) // basic 인증
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED) // 폼이라고 알린다.
                    .body(form) // 폼 본문
                    .retrieve() //응답을 받는다.
                    .body(XTokenResponse.class);
            // 응답이 비었을때, 연결 토큰이 비었을때 실패 처리를 한다.
            if(response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new BusinessException(ErrorCode.X_OAUTH_INVALID);
            }
            return response;
        }catch(RestClientResponseException e){
            // 응답 본문은 보안상의 이유로 남기지 않는다.
            log.warn("X OAuth 토큰 교환 실패. httpStatus : {}", e.getStatusCode());
            throw new BusinessException(ErrorCode.X_OAUTH_INVALID);
        }catch(RestClientException e){
            log.warn("X OAuth 토큰이 네트워크, 클라이언트 통신 에러로 인해 발급 실패 : {}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.X_OAUTH_INVALID);
        }
    }
}
