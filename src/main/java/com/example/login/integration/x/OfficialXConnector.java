package com.example.login.integration.x;

import com.example.login.common.BusinessException;
import com.example.login.integration.x.client.XTokenClient;
import com.example.login.integration.x.dto.Verified;
import com.example.login.integration.x.dto.XConnectionStatusResponse;
import com.example.login.integration.x.dto.XTokenResponse;
import com.example.login.integration.x.dto.XUserInfo;
import com.example.login.integration.x.entity.ExternalAccount;
import com.example.login.integration.x.repository.ExternalAccountRepository;
import com.example.login.integration.x.service.XAccountService;
import com.example.login.integration.x.service.XOAuthService;
import com.example.login.integration.x.token.TokenCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
@RequiredArgsConstructor
public class OfficialXConnector implements XConnector{
    private final XOAuthService xoAuthService;
    private final ExternalAccountRepository externalAccountRepository;
    private final XTokenClient xTokenClient;
    private final XAccountService xAccountService;
    private final TokenCipher tokenCipher;
    @Override
    public ConnectResult connect(Long userId) {
        Optional<ExternalAccount> status =  externalAccountRepository.findByUserIdAndProvider(userId, Provider.X.getProvider());
        // 이미 연결돼 있으면 새로 시작하지 않고 바로 돌려준다
        if(status.isPresent()){
            return new ConnectResult(new XConnectionStatusResponse(true, status.get().getUsername()), null);
        }
        // connect 는 "시작"만 한다. DB 저장은 허용을 누르고 돌아온 뒤(complete)라서 여기서 저장하지 않는다.
        XOAuthService.Start start = xoAuthService.start(userId);

        //ExternalAccount saved = externalAccountRepository.save(ExternalAccount.createFakeX(userId));
        //return new XConnectionStatusResponse(true, saved.getUsername());
        // authorizeUrl 은 브라우저가 이동할 주소라 응답 본문에, browserToken 은 쿠키에. 둘은 같은 start 에서 나온 짝이어야 한다
        // (start 를 두 번 부르면 난수가 새로 만들어져 쿠키와 주소의 짝이 안 맞는다)
        return new ConnectResult(new XConnectionStatusResponse(false, null, start.authorizeUrl()), start.browserToken());
    }

    @Override
    public boolean complete(String code, String state, String browserToken) {
        try {
            // codeVerifier: 토큰 교환 때 X 에 내는 원본. userId: 토큰을 누구 계정으로 저장할지
            Verified verified = xoAuthService.verify(state, browserToken);
            // 이 응답 개체는 어딘가로 가야한다. 하지만 이 메서드의 반환값을 바꾸어
            // 웹 계층까지 올라가면 안된다. 쓰지도 않는 곳에 비밀값이 퍼지기 때문이다.
            XTokenResponse response = xTokenClient.exchangeCode(code, verified.codeVerifier());
            XUserInfo userInfo = xTokenClient.getMyInfoForX(response.accessToken());
            xAccountService.save(verified.userId(), response, userInfo);
            return true;
        } catch (BusinessException | DataIntegrityViolationException e) {
            // 여기서 잡지 않으면 GlobalExceptionHandler 가 JSON 에러를 보내 사용자가 글자만 있는 화면에 갇힌다
            return false;
        }
    }

    /**
     * 연결 해제 전에 X 의 토큰을 폐기한다. 순서가 중요하다:
     *   행 삭제 -> 폐기 : 지운 뒤에는 refresh token 을 꺼낼 수 없다
     *   폐기 -> 행 삭제 : O  (그래서 컨트롤러가 이 메서드를 먼저 부른다)
     *
     * 트랜잭션을 걸지 않는다: X 를 기다리는 동안(최대 수 초) DB 커넥션을 붙잡지 않으려고. 조회 한 번만 한다.
     * 어떤 이유로든 실패해도 예외를 밖으로 내보내지 않는다 (XConnector.revoke 참고).
     */
    @Override
    public void revoke(Long userId) {
        Optional<ExternalAccount> account = externalAccountRepository.findByUserIdAndProvider(userId, Provider.X.getProvider());
        // 연결이 없거나 refresh token 이 없는 행(예전 fake 행 등)이면 X 에 알릴 것이 없다
        if (account.isEmpty() || account.get().getRefreshToken() == null) {
            return;
        }
        try {
            // DB 에는 잠긴(암호화된) 값이 있다 -> X 에 보내기 전에 푼다
            String refreshToken = tokenCipher.decrypt(account.get().getRefreshToken());
            boolean revoked = xTokenClient.revoke(refreshToken);
            log.info("X 연결 해제 userId={}, X 토큰 폐기={}", userId, revoked ? "성공" : "실패(행은 그대로 삭제)");
        } catch (IllegalStateException e) {
            // 복호화 실패: 암호화 키가 바뀐 경우 등. 우리가 풀 수 없는 토큰은 X 에 보낼 수도 없다
            log.warn("X 토큰 폐기 건너뜀(복호화 실패) userId={}", userId);
        }
    }
}
