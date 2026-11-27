package com.example.login.integration.x.client;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.integration.x.dto.XUserInfo;
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
    private static final String REVOKE_URL = "https://api.x.com/2/oauth2/revoke";

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

    public XUserInfo getMyInfoForX(String accessToken) {
        try{
            // Get 시작
            XUserInfo response = restClient.get()
                    .uri("https://api.x.com/2/users/me")
                    .headers(headers -> headers.setBearerAuth(accessToken)) // bearer 인증
                    .retrieve() //응답을 받는다.
                    .body(XUserInfo.class);
            // 응답이 비었을때, data 속 정보들이 비었을때 실패 처리를 한다.
            if(response == null
                    || response.data() == null
                    || response.data().id() == null
                    || response.data().id().isBlank()) {
                throw new BusinessException(ErrorCode.X_OAUTH_INVALID);
            }
            return response;
        }catch(RestClientResponseException e){
            // 응답 본문은 보안상의 이유로 남기지 않는다.
            log.warn("X 유저 정보 인증 실패. httpStatus : {}", e.getStatusCode());
            throw new BusinessException(ErrorCode.X_OAUTH_INVALID);
        }catch(RestClientException e){
            log.warn("X 유저 정보가, 네트워크 등 기타 요인으로 인해 인증 실패 : {}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.X_UNAVAILABLE);
        }
    }
    public XTokenResponse refresh(String refreshToken){
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        try{
            // POST로 시작.
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
            if(e.getStatusCode().is4xxClientError()){
                log.warn("X OAuth 토큰 갱신 실패. httpStatus : {}", e.getStatusCode());
                throw new BusinessException(ErrorCode.X_TOKEN_EXPIRED);
            }
            else{
                log.warn("X OAuth 토큰이 통신, 응답 실패로 인해 갱신 실패 : {}", e.getClass().getSimpleName());
                throw new BusinessException(ErrorCode.X_UNAVAILABLE);
            }
        }catch(RestClientException e){
            log.warn("X OAuth 토큰이 통신, 응답 실패로 인해 갱신 실패 : {}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.X_UNAVAILABLE);
        }

    }

    /**
     * 연결 해제 때 X 에 "이 토큰은 이제 쓰지 않는다" 고 알린다 (토큰 폐기).
     *
     * 왜 refresh token 을 폐기하나: access token 만 폐기하면 refresh token 으로 새 access token 을 다시 받을 수 있다.
     *   refresh token 을 폐기하면 그 연결에서 나온 토큰 전체가 끊긴다.
     *
     * 왜 예외를 던지지 않나 (최선을 다하되 실패해도 넘어간다):
     *   사용자는 "끄기" 를 눌렀다. X 가 잠깐 죽어 있다고 연결 해제를 실패시키면, 사용자는 끄지도 못하고
     *   DB 에는 쓰지 않을 토큰이 계속 남는다. 우리 쪽 행을 지우는 것이 더 중요하고, X 쪽 토큰은
     *   폐기하지 못해도 사용자가 X 설정에서 앱 권한을 직접 끊을 수 있다. 그래서 실패는 로그만 남긴다.
     *
     * @return 폐기 요청이 성공했으면 true (호출하는 쪽은 결과와 관계없이 진행한다. 시험·로그용)
     */
    public boolean revoke(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", refreshToken);
        // 어떤 종류의 토큰인지 알려 주면 X 가 더 빨리 찾는다 (힌트라서 없어도 동작은 한다)
        form.add("token_type_hint", "refresh_token");

        try {
            restClient.post()
                    .uri(REVOKE_URL)
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret)) // 우리 앱이 보낸 요청임을 증명
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity(); // 받을 본문이 없다. 2xx 면 성공, 4xx·5xx 면 아래 catch
            return true;
        } catch (RestClientResponseException e) {
            // 400 등: 이미 폐기됐거나 만료된 토큰. 어차피 쓸 수 없는 토큰이라 결과는 같다
            log.warn("X 토큰 폐기 실패(연결 해제는 계속 진행). httpStatus={}", e.getStatusCode());
            return false;
        } catch (RestClientException e) {
            // 통신 실패·타임아웃
            log.warn("X 토큰 폐기 실패(연결 해제는 계속 진행). cause={}", e.getClass().getSimpleName());
            return false;
        }
    }
}
