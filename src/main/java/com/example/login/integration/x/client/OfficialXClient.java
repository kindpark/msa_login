package com.example.login.integration.x.client;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.integration.x.Provider;
import com.example.login.integration.x.dto.XAuthor;
import com.example.login.integration.x.dto.XPost;
import com.example.login.integration.x.dto.XResponse;
import com.example.login.integration.x.dto.XTimelineResponse;
import com.example.login.integration.x.entity.ExternalAccount;
import com.example.login.integration.x.repository.ExternalAccountRepository;
import com.example.login.integration.x.token.TokenCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
public class OfficialXClient implements XTimelineClient{
    private final ExternalAccountRepository externalAccountRepository;
    private final TokenCipher tokenCipher;
    private final RestClient restClient;
    private static final String X_HOST = "https://x.com/";
    public OfficialXClient(
            ExternalAccountRepository externalAccountRepository,
            TokenCipher tokenCipher
    ){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(1));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.externalAccountRepository = externalAccountRepository;
        this.tokenCipher = tokenCipher;
        this.restClient = RestClient
                .builder()
                .requestFactory(requestFactory)
                .build();
    }
    @Override
    public XTimelineResponse fetchTimeline(Long userId, String cursor, int size){
        return getXTimeline(userId, cursor, size);
    }

    private XTimelineResponse getXTimeline(
            Long userId,
            String cursor,
            int size
    ) {
        int pageSize = Math.min(Math.max(size, 1), 20);
        // account 확인
        ExternalAccount account = externalAccountRepository.findByUserIdAndProvider(userId, Provider.X.getProvider())
                .orElseThrow(() -> new BusinessException(ErrorCode.X_NOT_CONNECTED));
        // 토큰이 만료되었다면 자격 상실 에러 보냄
        if (account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.X_TOKEN_EXPIRED);
        }
        // 토큰 복호화
        String accessToken = tokenCipher.decrypt(account.getAccessToken());
        XResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.x.com")
                            .path("/2/users/{id}/timelines/reverse_chronological")
                            .queryParam("exclude", "replies,retweets")
                            .queryParam("max_results", pageSize)
                            .queryParam("tweet.fields", "created_at,author_id")
                            .queryParam("expansions", "author_id")
                            .queryParam("user.fields", "username,name,profile_image_url")
                            .queryParamIfPresent("pagination_token", Optional.ofNullable(cursor))
                            //getId -> 우리DB의 식별자
                            //getUserId -> 마찬가지
                            //external -> X가 준 내 번호. 이것이 진짜 식별자
                            .build(account.getExternalUserId()))
                    .headers(headers -> headers.setBearerAuth(accessToken)) // bearer 인증
                    .retrieve() //응답을 받는다.
                    .body(XResponse.class);
        // 로그 규칙: 토큰·응답 본문은 남기지 않는다(본문에 사용자 정보가 섞일 수 있다). 누구의 요청인지(userId)와 상태 코드만.
        }catch(HttpClientErrorException.Unauthorized e){
            // 401: 토큰이 만료·폐기됨 (사용자가 X 에서 앱 권한을 끊은 경우 포함) -> 다시 연결해야 한다
            log.warn("X 타임라인 조회 실패(토큰 거절) userId={}, httpStatus={}", userId, e.getStatusCode());
            throw new BusinessException(ErrorCode.X_TOKEN_EXPIRED);
        }catch(HttpClientErrorException.TooManyRequests e){
            // 429: X 쪽 요청 한도(15분 180회) 초과. 우리 daily-limit 과는 다른 한도다
            log.warn("X 타임라인 조회 실패(X 요청 한도 초과) userId={}, httpStatus={}", userId, e.getStatusCode());
            throw new BusinessException(ErrorCode.X_RATE_LIMITED);
        }catch(RestClientResponseException e){
            // 그 밖의 응답: 402(크레딧 없음), 403(권한 없음), 400(잘못된 요청), 5xx(X 장애) 등. 상태 코드로 원인을 가린다
            log.warn("X 타임라인 조회 실패 userId={}, httpStatus={}", userId, e.getStatusCode());
            throw new BusinessException(ErrorCode.X_UNAVAILABLE);
        }catch(RestClientException e){
            // 응답 자체가 없거나 읽지 못함: 연결 실패, 타임아웃, 깨진 JSON
            log.warn("X 타임라인 조회 실패(통신·응답 해석) userId={}, cause={}", userId, e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.X_UNAVAILABLE);
        }
        String cur = (response != null && response.meta() != null)
                ? response.meta().next_token()
                : null;
        if(response == null
                    || response.data() == null
                    || response.data().isEmpty()) {
            return new XTimelineResponse(List.of(), cur);
        }
        // include user -> Map 변환
        Map<String, XResponse.UserInclude.XUsers> userMap = Optional.ofNullable(
                response.includes()
        ).map(XResponse.UserInclude::users)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(
                        XResponse.UserInclude.XUsers::id,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        // data를 순서로 돌며 author_id로 작성자 조회 후 XPost 리스트 생성
        List<XPost> items = response.data().stream()
                .map(tweet ->{
                    XResponse.UserInclude.XUsers author = userMap.get(tweet.author_id());

                    // UTC 기준 offsetDateTime을 kst의 로컬타임으로 변환
                    LocalDateTime createdAt = tweet.created_at() != null
                            ? tweet.created_at().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()
                            : null;
                    // 1. author가 있으면 4개 필드 모두 채우고, 없으면 id만 세팅
                    XAuthor xAuthor = author != null
                            ? new XAuthor(
                            author.id(),
                            author.username(),
                            author.name(),
                            author.profile_image_url()
                    )
                            : new XAuthor(tweet.author_id(), null, null, null);
                    // 2. author가 있으면 username 주소, 없으면 /i/ 주소 생성
                    String url = author != null
                            ? X_HOST + author.username() + "/status/" + tweet.id()
                            : X_HOST + "i/status/" + tweet.id();
                    return new XPost(
                            tweet.id(),
                            tweet.text(),
                            createdAt,
                            xAuthor,
                            url
                    );
                }).toList();
        return new XTimelineResponse(items, cur);
    }
}
