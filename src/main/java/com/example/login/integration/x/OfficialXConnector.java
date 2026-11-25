package com.example.login.integration.x;

import com.example.login.integration.x.dto.XConnectionStatusResponse;
import com.example.login.integration.x.entity.ExternalAccount;
import com.example.login.integration.x.repository.ExternalAccountRepository;
import com.example.login.integration.x.service.XOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
@RequiredArgsConstructor
public class OfficialXConnector implements XConnector{
    private final XOAuthService xoAuthService;
    private final ExternalAccountRepository externalAccountRepository;

    @Override
    public ConnectResult connect(Long userId) {
        XConnectionStatusResponse status =  externalAccountRepository.findByUserIdAndProvider(userId, Provider.X.getProvider())
                .map(account -> new XConnectionStatusResponse(true, account.getUsername()))
                .orElseGet(() -> {
                    try {
                        XOAuthService.Start start = xoAuthService.start(userId);

                        //ExternalAccount saved = externalAccountRepository.save(ExternalAccount.createFakeX(userId));
                        //return new XConnectionStatusResponse(true, saved.getUsername());
                    } catch (DataIntegrityViolationException e) {
                        // 동시 요청으로 이미 생성된 경우 재조회하여 정상 응답
                        ExternalAccount existing = externalAccountRepository.findByUserIdAndProvider(userId, Provider.X.getProvider())
                                .orElseThrow();
                        return new XConnectionStatusResponse(true, existing.getUsername());
                    }
                });
        return new ConnectResult(, null);
    }
}
