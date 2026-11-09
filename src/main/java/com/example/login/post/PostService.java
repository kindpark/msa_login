package com.example.login.post;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    // 내꺼 조회
    @Transactional(readOnly = true)
    public List<PostResponseDto> getMyPost(Long userId){
        //내 글목록을 조회
        List<Post> posts = postRepository.findByUserId(userId);
        return posts.stream()
                .map(PostResponseDto::from)
                .toList();
    }

    // 전체 조회
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPost(){
        return postRepository.findAll()
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }
    // 상세 조회
    @Transactional(readOnly = true)
    public PostResponseDto getDetailPost(Long id){
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        return new PostResponseDto(
                post.getId(),
                post.getUserId(),
                post.getContent(),
                post.getImageUrl(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt());

    }
    //좋아요 토글

    //댓글작성

    // 댓글 목록

    //C
    @Transactional
    public PostResponseDto createPostItem(PostRequestDto dto, Long userId){
        Post post = Post.builder()
                .userId(userId)
                .content(dto.getContent())
                .imageUrl(dto.getImageUrl())
                .build();
        Post save = postRepository.save(post);
        return PostResponseDto.from(save);
    }
    //U
    @Transactional
    public PostResponseDto updatePostItem(Long id, Long userId, PostRequestDto dto){
        Post post = postRepository.findById(id)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.update(userId, dto.getContent(), dto.getImageUrl());
        return PostResponseDto.from(post);
    }
    //D
    @Transactional
    public PostResponseDto deletePostItem(Long id, Long userId){
        // 삭제할 게시글이 존재하는지 확인 및 조회
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 권한 검증: 게시글의 작성자와 현재 로그인한 유저의 ID가 일치하는지 확인
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 검증을 통과하면 데이터베이스에서 삭제 진행
        postRepository.delete(post);

        // 삭제된 게시글 정보를 DTO로 변환하여 반환
        return PostResponseDto.from(post);
    }
}
