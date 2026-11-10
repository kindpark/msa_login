package com.example.login.postlike;

import com.example.login.follow.FollowResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {
    private final PostLikeService postLikeService;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<PostLikeResponseDto> toggleLike(
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
    //포스트의 좋아요 보기(내꺼, 남꺼 다 보이게)
    @GetMapping("/postlike")
    public ResponseEntity<List<Long>> getPostLike(
            @PathVariable Long postId
    ){
        return ResponseEntity.ok(postLikeService.getPostLikeService(postId));
    }
}
