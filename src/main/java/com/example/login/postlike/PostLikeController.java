package com.example.login.postlike;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {
    private final PostLikeService postLikeService;

    @PostMapping
    public ResponseEntity<PostLikeResponseDto> toggleLike(
            // 경로에 있는 targetUserId가 pathvariable이다.
            // 내 아이디는 로그인 기반이므로 Auth..인데 이를 오해함
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
            ){
        try{
            boolean result = postLikeService.toggle(postId, userId);
            return ResponseEntity.ok(new PostLikeResponseDto(result));
        }catch(DataIntegrityViolationException e){
            return ResponseEntity.ok(new PostLikeResponseDto(true));
        }
    }
    /*
    //포스트의 좋아요 누른 사람 보기(추후 사용)
    @GetMapping
    public ResponseEntity<List<Long>> getPostLike(

            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(postLikeService.getLikePosts(posts, userId));
    }

     */
}
