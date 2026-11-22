package com.example.login.integration.x.service;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.integration.x.client.XTimelineClient;
import com.example.login.integration.x.dto.XConnectionStatusResponse;
import com.example.login.integration.x.dto.XTimelineResponse;
import com.example.login.integration.x.entity.ExternalAccount;
import com.example.login.integration.x.repository.ExternalAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException; // Jackson 3부터는 RuntimeException인 JacksonException을 주로 사용
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class XTimelineService {

    // X 프로바이더 추가
    private static final String PROVIDER_X = "X";

    private final XTimelineClient xTimelineClient;
    private final ExternalAccountRepository externalAccountRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${integration.x.enabled:false}")
    private boolean enabled;
    @Value("${integration.x.client:fake}")
    private String clientType;

    @Value("${integration.x.daily-limit:100}")
    private long dailyLimit;

    @Transactional(readOnly = true)
    public XConnectionStatusResponse getConnectionStatus(Long userId) {
        return externalAccountRepository.findByUserIdAndProvider(userId, PROVIDER_X)
                .map(account -> new XConnectionStatusResponse(true, account.getUsername()))
                .orElseGet(() -> new XConnectionStatusResponse(false, null));
    }

    public XConnectionStatusResponse connect(Long userId) {
        return externalAccountRepository.findByUserIdAndProvider(userId, PROVIDER_X)
                .map(account -> new XConnectionStatusResponse(true, account.getUsername()))
                .orElseGet(() -> {
                    try {
                        ExternalAccount saved = externalAccountRepository.save(ExternalAccount.createFakeX(userId));
                        return new XConnectionStatusResponse(true, saved.getUsername());
                    } catch (DataIntegrityViolationException e) {
                        // 동시 요청으로 이미 생성된 경우 재조회하여 정상 응답
                        ExternalAccount existing = externalAccountRepository.findByUserIdAndProvider(userId, PROVIDER_X)
                                .orElseThrow();
                        return new XConnectionStatusResponse(true, existing.getUsername());
                    }
                });
    }
    @Transactional
    public void disconnect(Long userId) {
        externalAccountRepository.deleteByUserIdAndProvider(userId, PROVIDER_X);
    }
    public XTimelineResponse getTimeline(Long userId, String cursor, int size){
        // DB 행 존재 여부로 연동 확인 (없으면 409 X_NOT_CONNECTED)
        ExternalAccount account = externalAccountRepository.findByUserIdAndProvider(userId, PROVIDER_X)
                .orElseThrow(() -> new BusinessException(ErrorCode.X_NOT_CONNECTED));

        String normalizedCursor = (cursor==null || cursor.isBlank())
                ? "initial"
                : cursor;
        String cacheKey = String.format("x:timeline:%d:%s:%d", userId, normalizedCursor, size);

        // 캐시를 먼저 확인한다.
        String cacheJson = redisTemplate.opsForValue().get(cacheKey);
        if(cacheJson != null){
            try{
                return objectMapper.readValue(cacheJson, XTimelineResponse.class);
            }catch(JacksonException e){
                log.warn("X 타임라인 캐시 역직렬화 실패, 원본 API 호출로 진행: key={}", cacheKey);
            }
        }
        // 캐시 미스일때 일일 상한 확인 및 증가시키는 역할의 메서드
        checkAndIncrementDailyLimit();

        // X API 호출
        XTimelineResponse response = fetchWithTimeout(userId, cursor, size);

        // 실제 호출 성공 로그
        log.info("X API 호출 성공 - userId = {}, fetchedCount = {}", userId, response.getItems().size());
        // 캐시 저장(TTL 60초..?)
        try{
            String jsonToCache = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, jsonToCache, Duration.ofSeconds(60));
        }catch(JacksonException e){
            log.warn("X 타임라인 캐시 직렬화 실패: key={}", cacheKey);
        }
        return response;
    }
    // 캐시 미스일때 일일 상한을 확인하고 증가시키는 역할의 메서드
    private void checkAndIncrementDailyLimit(){
        String limitKey = String.format("x:daily_limit:%s", LocalDate.now());
        Long currentCount = redisTemplate.opsForValue().increment(limitKey);

        if(currentCount != null && currentCount == 1L){
            LocalDateTime midnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
            Duration ttlUntilMidnight = Duration.between(LocalDateTime.now(), midnight);
            redisTemplate.expire(limitKey, ttlUntilMidnight);
        }
        if(currentCount != null && currentCount > dailyLimit){
            throw new BusinessException(ErrorCode.X_RATE_LIMITED);
        }
    }

    private XTimelineResponse fetchWithTimeout(Long userId, String cursor, int size) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> xTimelineClient.fetchTimeline(userId, cursor, size))
                    .get(3, TimeUnit.SECONDS); // 3초 타임아웃
        } catch (TimeoutException e) {
            throw new BusinessException(ErrorCode.X_UNAVAILABLE);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BusinessException xEx) {
                throw xEx; // X_TOKEN_EXPIRED, X_UNAVAILABLE 등 원래 예외 그대로 전파
            }
            throw new BusinessException(ErrorCode.X_UNAVAILABLE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.X_UNAVAILABLE);
        }
    }
}
