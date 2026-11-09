package com.example.login.postlike;

import com.example.login.follow.FollowResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/postlike")
@RequiredArgsConstructor
public class PostLikeController {
    private PostLikeService postLikeService;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<PostLikeResponseDto> toggleFollow(
            // 경로에 있는 targetUserId가 pathvariable이다.
            // 내 아이디는 로그인 기반이므로 Auth..인데 이를 오해함
            @AuthenticationPrincipal Long id,
            @PathVariable Long targetUserId){
        try{
            boolean result = postLikeService.toggle(id, targetUserId);
            return ResponseEntity.ok(new PostLikeResponseDto(result));
        }catch(DataIntegrityViolationException e){
            return ResponseEntity.ok(new PostLikeResponseDto(true));
        }
    }

}
