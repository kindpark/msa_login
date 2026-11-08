package com.example.login.follow;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<FollowResponseDto> toggleFollow(
            // 경로에 있는 targetUserId가 pathvariable이다.
            // 내 아이디는 로그인 기반이므로 Auth..인데 이를 오해함
            @AuthenticationPrincipal Long id,
            @PathVariable Long targetUserId){
        try{
            boolean result = followService.toggle(id, targetUserId);
            return ResponseEntity.ok(new FollowResponseDto(result));
        }catch(DataIntegrityViolationException e){
            return ResponseEntity.ok(new FollowResponseDto(true));
        }
    }

    @GetMapping("/followings")
    public List<Long> getFollowing(
            @AuthenticationPrincipal Long id
    ){
        return followService.getMyFollowing(id);
    }
}
