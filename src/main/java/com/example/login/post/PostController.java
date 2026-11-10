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
    public ResponseEntity<List<PostResponseDto>> readAllPosts(){
        return ResponseEntity.ok(postService.getAllPost());
    }
    // 게시글 상세
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> readDetailPost(
            @PathVariable Long id){
        return ResponseEntity.ok(postService.getDetailPost(id));
    }
    /*
    // 좋아요 토글(이제 필요없음)
    @GetMapping("/{postId}/likes")
    public ResponseEntity<PostResponseDto> readDetailPostLike(){
        return ResponseEntity.ok(postService.getDetailPostLike());
    }
    */

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
    // 댓글 불러오기
    @GetMapping("/{postId}/replies")
    public ResponseEntity<List<PostResponseDto>> commentList(
            @PathVariable Long postId
    ){
        return ResponseEntity.ok(postService.getReplys(postId));
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
