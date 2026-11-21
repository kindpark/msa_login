package com.example.login.integration.x.controller;


import com.example.login.integration.x.dto.XConnectionStatusResponse;
import com.example.login.integration.x.dto.XTimelineResponse;
import com.example.login.integration.x.service.XTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations/x")
@RequiredArgsConstructor
public class XIntegrationController {

    private final XTimelineService xTimelineService;
// 인증된 사용자 ID 추출 부분은 프로젝트의 시큐리티 설정에 맞게 연결
    //우리는 userId인데, 여기서도 이게 맞는지 의문
    @GetMapping
    public ResponseEntity<XConnectionStatusResponse> getConnectionStatus(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(xTimelineService.getConnectionStatus(userId));
    }

    @PostMapping("/connect")
    public ResponseEntity<XConnectionStatusResponse> connect(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(xTimelineService.connect(userId));
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
}
