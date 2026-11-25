package com.example.login.integration.x;

import com.example.login.common.BusinessException;
import com.example.login.integration.x.dto.XConnectionStatusResponse;
import com.example.login.integration.x.entity.ExternalAccount;
import com.example.login.integration.x.repository.ExternalAccountRepository;
import com.example.login.integration.x.service.XOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
@RequiredArgsConstructor
public class OfficialXConnector implements XConnector{
    private final XOAuthService xoAuthService;
    private final ExternalAccountRepository externalAccountRepository;

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
            xoAuthService.verify(state, browserToken);
            return true;
        } catch (BusinessException e) {
            // 여기서 잡지 않으면 GlobalExceptionHandler 가 JSON 에러를 보내 사용자가 글자만 있는 화면에 갇힌다
            return false;
        }
    }
}
