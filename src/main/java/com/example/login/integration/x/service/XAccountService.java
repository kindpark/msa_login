package com.example.login.integration.x.service;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.integration.x.Provider;
import com.example.login.integration.x.dto.XTokenResponse;
import com.example.login.integration.x.dto.XUserInfo;
import com.example.login.integration.x.entity.ExternalAccount;
import com.example.login.integration.x.repository.ExternalAccountRepository;
import com.example.login.integration.x.token.TokenCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
public class XAccountService {
    private final ExternalAccountRepository externalAccountRepository;
    private final TokenCipher tokenCipher;
    // XTokenClient에서 정보들을 저장
    @Transactional
    public void save(Long userId,
                            XTokenResponse response,
                            XUserInfo info){

        String accessToken = response.accessToken();
        String accessTokenEncrypt = tokenCipher.encrypt(accessToken);
        String refreshToken = response.refreshToken();
        String refreshTokenEncrypt = tokenCipher.encrypt(refreshToken);

        externalAccountRepository.save(
        ExternalAccount.createX(
                userId,
                info.data().username(),
                info.data().id(),
                accessTokenEncrypt,
                refreshTokenEncrypt,
                LocalDateTime.now().minusSeconds(60).plusSeconds(response.expiresIn())
        ));

    }
    @Transactional
    public void updateTokens(Long userId, XTokenResponse response){
        ExternalAccount account = externalAccountRepository.findByUserIdAndProvider(userId, Provider.X.getProvider())
                .orElseThrow(()-> new BusinessException(ErrorCode.X_OAUTH_INVALID));

        String access = tokenCipher.encrypt(response.accessToken());
        String refresh = tokenCipher.encrypt(response.refreshToken());

        account.setTokens(access, refresh, LocalDateTime.now().minusSeconds(60).plusSeconds(response.expiresIn()));

    }
}
