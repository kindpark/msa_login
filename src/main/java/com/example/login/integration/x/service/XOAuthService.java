package com.example.login.integration.x.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
public class XOAuthService {
    // 보안 관련해서는 일반난수(운영체제를 배우면 seed값으로, 시간에 맞게 난수를 주는
    // 일반 random에 대해 설명을 듣는데, 해당 규칙을 아는 사람에게 다음 난수값이 예측될수 있어
    // 일반 랜덤값은 쓰이지 않음
    private static final SecureRandom random = new SecureRandom();
    // redis
    private final StringRedisTemplate redisTemplate;
    // 직렬화 mapper
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String redirectUrl;

    public XOAuthService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${integration.x.client-id}")String clientId,
            @Value("${integration.x.redirect-uri}") String redirectUrl
    ){
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clientId =clientId;
        this.redirectUrl = redirectUrl;
    }
    // 랜덤 토큰값을 제공한다.
    // 부를때마다 다른 걸 도출해내야 하기 때문에 합치면 안됨(여러번 쓰일수도)
    private static String getRandomToken(){
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    // sha256방식으로 해싱
    private static String sha256(String value){
        try {
            // sha256받식 해싱
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        }catch(NoSuchAlgorithmException e){
            throw new RuntimeException("SHA-256 알고리즘을 찾을수 없습니다.", e);
        }
    }
    // 두 String을 반환하기 위한 레코드
    public record Start(String authorizeUrl, String browserToken){}

    // X 로그인 주소 만든다.
    public Start start(Long userId) {
        // 1. 재료 준비
        String state = getRandomToken();
        String codeVerifier = getRandomToken();
        String browserToken = getRandomToken();
        String codeChallenge = sha256(codeVerifier);

        // 2. Redis에 10분 보관
        // redis 키 생성
        String redisKey = "oauth:x:state:" + state;
        // 콜백 시 검증, 토큰 교환에 필요한 데이터 묶기
        Map<String, String> authData = Map.of(
                "userId", String.valueOf(userId),
                "codeVerifier", codeVerifier,
                "browserToken", browserToken
        );

        // json 문자열로 변환 후 저장
        String jsonValue = objectMapper.writeValueAsString(authData);
        redisTemplate.opsForValue().set(redisKey, jsonValue, Duration.ofMinutes(10));
        // 주소를 조립
        String authUrl = UriComponentsBuilder.fromUriString("https://x.com/i/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("scope", "tweet.read users.read offline.access") // 필요한 권한(Scope)
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode()
                .build()
                .toUriString();
        return new Start(authUrl, browserToken);
    }
}
