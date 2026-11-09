package com.example.login.post;

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
    //내꺼조회
    @GetMapping("/me")
    public ResponseEntity<List<PostResponseDto>> readOnMyPost(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(postService.getMyPost(userId));
    }
    //전체조회
    @GetMapping("/all")
    public ResponseEntity<List<PostResponseDto>> readAllPosts(){
        return ResponseEntity.ok(postService.getAllPost());
    }
    //게시글 상세
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> readDetailPost(
            @PathVariable Long id){
        return ResponseEntity.ok(postService.getDetailPost(id));
    }
    /*
    // 좋아요 토글
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
    /*
    //댓글 작성
    @PostMapping("/{postId}/comments")
    public ResponseEntity<PostRequestDto> writeComments(
            @RequestParam PostRequestDto dto,
            @PathVariable Long id
    ){
        return ResponseEntity.ok(postService.createCommentItem());
    }
    //댓글 목록
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<PostResponseDto>> commentList(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(postService.getAllComments());
    }

     */
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
