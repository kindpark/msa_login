package com.example.login.repost;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/reposts")
@RequiredArgsConstructor
public class RepostController {
    private final RepostService repostService;

    // 재게시를 하는 컨트롤러
    @PostMapping
    public ResponseEntity<RepostResponseDto> toggle(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ){
        return ResponseEntity.ok(repostService.repost(userId, postId));
    }
}
