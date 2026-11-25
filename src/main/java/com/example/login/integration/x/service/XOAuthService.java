package com.example.login.integration.x.service;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.integration.x.dto.Verified;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * X 로그인(OAuth 2.0 + PKCE)의 "시작"과 "돌아왔을 때 확인"을 맡는다.
 *
 * 전체 흐름
 *  1) start  : 난수를 만들어 Redis 에 보관하고, X 로그인 주소를 만든다
 *  2) 사용자가 X 화면에서 "허용"
 *  3) X 가 우리 /callback 으로 code(교환권)와 state 를 들고 돌려보낸다
 *  4) verify : state 로 Redis 에서 꺼내 "우리가 시작한 요청, 같은 브라우저"인지 확인한다
 *  5) code + codeVerifier 를 X 에 보내 토큰으로 바꾼다 (다음 단계)
 *
 * 난수 세 개의 역할
 *  - state        : 주소에 공개. 우리가 시작하지 않은 콜백을 걸러낸다 (Redis 에 없으면 가짜)
 *  - codeVerifier : Redis 에만 숨김. 가로챈 code 를 남이 쓰지 못하게 한다
 *  - browserToken : 쿠키 + Redis. 다른 사람의 브라우저에서 이어 하지 못하게 한다
 *
 * ConditionalOnProperty: official 일 때만 빈이 된다. fake 환경(키가 없는 Docker·테스트)에서도 앱이 뜨게 하려는 것.
 * 그래서 컨트롤러가 이 클래스를 직접 주입받으면 fake 일 때 앱이 안 뜬다 -> XConnector 를 거친다.
 */
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
    // static: 이 객체의 필드(redis, clientId)를 쓰지 않는 도구라서.  private: 이 클래스 안에서만 쓰므로.
    // 32바이트 -> Base64Url 로 적으면 43글자. codeVerifier 는 43글자 이상이어야 한다(PKCE 규칙).
    // 바이트 수와 글자 수는 다르다. 결과를 substring 으로 자르면 그만큼 추측하기 쉬워진다.
    // Base64Url: 일반 Base64 의 + / 를 - _ 로 바꾼 것. 주소에 넣어도 깨지지 않는다. withoutPadding 은 끝의 = 를 뺀다.
    // nextBytes 는 값을 돌려주지 않고 배열을 채운다.
    // 랜덤 토큰값을 제공한다.
    // 부를때마다 다른 걸 도출해내야 하기 때문에 합치면 안됨(여러번 쓰일수도)
    private static String getRandomToken(){
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    // 해시: 같은 값을 넣으면 항상 같은 결과, 결과에서 원래 값으로는 되돌릴 수 없다.
    //  - 주소에는 해시(codeChallenge)만 공개하고 원본(codeVerifier)은 숨긴다.
    //  - 나중에 원본을 내면 X 가 직접 해시해 비교한다 -> "처음 시작한 쪽"이라는 증명. 이것이 PKCE.
    //  - 전체를 비교하므로 한 글자라도 자르거나 Hex 로 적으면 실패한다.
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
    // record: 값만 담는 클래스를 짧게 쓰는 자바 문법 (생성자와 꺼내는 메서드가 자동). 역할로 보면 DTO.
    // 자바 메서드는 값을 하나만 돌려줄 수 있어 상자에 담는다. 꺼낼 때는 getX() 가 아니라 start.authorizeUrl().
    // browserToken 은 쿠키로만 보내야 하므로 이 레코드를 응답 본문으로 내보내면 안 된다.
    // 두 String을 반환하기 위한 레코드
    public record Start(String authorizeUrl, String browserToken){}

    // X 로그인 주소 만든다.
    public Start start(Long userId) {
        // 1. 재료 준비: 셋은 공개 범위가 달라 하나로 같이 쓰면 안 된다
        String state = getRandomToken();
        String codeVerifier = getRandomToken();
        String browserToken = getRandomToken();
        String codeChallenge = sha256(codeVerifier);

        // 2. Redis에 10분 보관
        // state 를 키로 쓰는 이유: X 가 state 를 그대로 돌려주므로 그걸로 누구의 요청인지 찾는다.
        // 10분: 로그인하다 만 요청이 계속 남지 않게. 지나면 verify 에서 "없음"이 된다.
        // codeChallenge 는 다시 쓸 일이 없어 저장하지 않는다.
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
        // 주소를 조립. 여기 적은 값은 사용자에게 모두 보인다 -> codeVerifier, browserToken 은 넣지 않는다.
        // redirect_uri 는 X 콘솔에 등록한 값과 글자까지 같아야 한다. encode() 는 scope 의 공백을 %20 으로 바꾼다.
        // offline.access: 2시간 뒤 만료되는 토큰을 다시 로그인 없이 갱신하기 위한 권한.
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

    /**
     * 콜백이 우리가 시작한 요청이 맞는지 확인한다.
     * 검사에 걸리면 반드시 거기서 끝나야 한다(throw / return). if 안이 비어 있으면 그냥 아래로 내려가 통과해 버린다.
     * boolean 이 아니라 Verified 를 돌려주는 이유: 토큰 교환에 userId 와 codeVerifier 가 필요한데,
     * getAndDelete 로 이미 지워서 다시 꺼낼 수 없다.
     */
    public Verified verify(String state,
                                     String browserToken){
        // 로그인 쿠키(user_token)는 SameSite=Strict 라 X 에서 돌아오는 요청에 붙지 않는다 -> 로그인 정보가 null.
        // 사용자는 redis의 userId로 찾기에 @AuthenticationPrincipal을 쓰면 안됨.
        String redisKey = "oauth:x:state:" + state;
        // 꺼내면서 동시에 지운다. get 다음 delete 로 나누면 그 사이에 같은 콜백이 또 들어올 수 있다 -> 한 번만 쓸 수 있게.
        // 꺼낸 것은 토큰 하나가 아니라 start 에서 넣은 JSON 전체다: {"userId":..,"codeVerifier":..,"browserToken":..}
        String saveRediskey = redisTemplate.opsForValue().getAndDelete(redisKey);

        // 만료됐거나, 위조된 state 이거나, 이미 한 번 쓴 경우
        if(saveRediskey == null){
            throw new BusinessException(ErrorCode.X_OAUTH_INVALID);
        }
        // TypeReference: Map.class 만 주면 안의 타입(String, String)을 알 수 없어서 타입 전체를 알려 준다
        Map<String, String> tokenData = objectMapper.readValue(saveRediskey, new TypeReference<Map<String, String>>(){});

        String savedToken = tokenData.get("browserToken");
        // 시작한 브라우저가 아니다 (콜백 주소를 다른 사람에게 열게 한 경우 등)
        if(!browserToken.equals(savedToken)){
            throw new BusinessException(ErrorCode.X_OAUTH_INVALID);
        }
        return new Verified(Long.valueOf(tokenData.get("userId")), tokenData.get("codeVerifier"));
    }
}
