package com.example.login.post;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    // 내꺼조회
    @GetMapping("/me")
    public ResponseEntity<List<PostResponseDto>> readOnMyPost(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(postService.getMyPost(userId));
    }
    // 전체조회
    @GetMapping("/all")
    public ResponseEntity<List<PostResponseDto>> readAllPosts(@AuthenticationPrincipal Long userId){
        return ResponseEntity.ok(postService.getAllPost(userId));
    }
    // 게시글 상세
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> readDetailPost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId){
        return ResponseEntity.ok(postService.getDetailPost(id, userId));
    }

    //C
    @PostMapping
    public ResponseEntity<PostResponseDto> writePost(
            @RequestBody PostRequestDto dto,
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(postService.createPostItem(dto, userId));
    }

    //댓글 작성(댓글이 아니라 X마냥 피드에 답 남기는)
    @PostMapping("/{postId}/replies")
    public ResponseEntity<PostResponseDto> writeReply(
            @RequestBody @Valid PostRequestDto dto,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(
                postService.createReply(
                        dto,
                        postId,
                        userId
                ));
    }
    // 답글 목록 (커서 페이지네이션, 오래된 순)
// 응답: { posts, nextCursor } — nextCursor 를 다음 요청의 cursor 로 넘기면 이어서 조회, null 이면 끝
// size: 기본 10, 서비스에서 최대 50 으로 제한
    @GetMapping("/{postId}/replies")
    public ResponseEntity<PostPageResponseDto> threadList(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ){
        return ResponseEntity.ok(postService.getReplys(userId, postId, cursor, size));
    }
    //수정
    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> updatePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody PostRequestDto dto) {
            return ResponseEntity.ok(postService.updatePostItem(id, userId, dto));
    }
    //삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<PostResponseDto> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(postService.deletePostItem(id, userId));
    }

}
