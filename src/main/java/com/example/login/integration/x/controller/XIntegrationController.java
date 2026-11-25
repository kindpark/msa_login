package com.example.login.integration.x.controller;


import com.example.login.integration.x.XConnector;
import com.example.login.integration.x.dto.XConnectionStatusResponse;
import com.example.login.integration.x.dto.XTimelineResponse;
import com.example.login.integration.x.service.XTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/integrations/x")
@RequiredArgsConstructor
public class XIntegrationController {

    private final XTimelineService xTimelineService;
    private final XConnector xConnector;
    // final 을 붙이지 않는 이유: final 이면 RequiredArgsConstructor 가 생성자 인수로 만드는데, 거기에는 @Value 가 옮겨지지 않는다
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    // 인증된 사용자 ID 추출 부분은 프로젝트의 시큐리티 설정에 맞게 연결
    //우리는 userId인데, 여기서도 이게 맞는지 의문
    @GetMapping
    public ResponseEntity<XConnectionStatusResponse> getConnectionStatus(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(xTimelineService.getConnectionStatus(userId));
    }
    //
    @PostMapping("/connect")
    public ResponseEntity<XConnectionStatusResponse> connect(
            @AuthenticationPrincipal Long userId
    ){// fake: 기존 동작 그대로
        XConnector.ConnectResult result = xConnector.connect(userId);
        if(result.cookie() == null){
            return ResponseEntity.ok(result.response());
        }

        // x_oauth: 시작한 브라우저를 확인하는 짧은 쿠키. 로그인 쿠키(user_token)와 다른 점
        //  - sameSite Lax : X 에서 돌아오는 이동(GET)에도 붙는다. Strict 면 안 붙는다
        //  - path         : 콜백 주소로 갈 때만 보낸다
        //  - httpOnly     : 자바스크립트가 읽을 수 없다
        //  - secure       : true 면 HTTPS 에서만 저장. 개발은 http 라 설정값으로 (배포하면 true)
        ResponseCookie cookie = ResponseCookie.from("x_oauth", result.cookie())
                .path("/api/v1/integrations/x/callback")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .maxAge(60 * 10)
                .build();
        // 쿠키는 만들기만 해서는 안 가고 Set-Cookie 헤더에 실어야 브라우저가 저장한다
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.response());
    }
    @DeleteMapping
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal Long userId
    ) {
        xTimelineService.disconnect(userId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/timeline")
    public ResponseEntity<XTimelineResponse> getTimeline(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 20); // 최소 1, 최대 20 제한

        XTimelineResponse response = xTimelineService.getTimeline(userId, cursor, boundedSize);
        return ResponseEntity.ok(response);
    }
    /**
     * X 에서 "허용"이나 "취소"를 누르면 브라우저가 이 주소로 돌아온다.
     *  - 로그인 쿠키가 오지 않으므로 SecurityConfig 에서 permitAll 이고, 사용자는 state 로 찾는다
     *  - 모두 required = false: 취소하면 code 없이 error 만 온다. 필수로 두면 처리하기 전에 400 이 난다
     *  - JSON 이 아니라 302(이동): 사람이 보는 화면이라 결과와 상관없이 X 탭으로 돌려보낸다
     *  - code, state, 쿠키 값은 로그에 남기지 않는다
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false)
            String code,
            @RequestParam(required = false)
            String state,
            @RequestParam(required = false)
            String error,
        @CookieValue(name="x_oauth", required = false)
            String browserToken
    ){
        boolean ok = error == null && code != null && state != null && browserToken != null
                && xConnector.complete(code, state, browserToken);
        String to = ok
                ? "/?tab=x"
                : "/?tab=x&xError=1";
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(to)).build();
    }
}
