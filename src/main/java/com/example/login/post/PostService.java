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

        return PostResponseDto.from(post);
    }
    //좋아요 토글(postlike로 주요 기능 이관)

    //댓글작성(단순 게시판의 답글 개념이 아니라, X마냥 피드의 답글을 남기는 개념)
    //long id가 글의 id
    //JPA의 @ManyToOne 같은 연관관계를 따로 만들 필요가 없습니다.
    // PostLike에서 postId를 숫자로 저장한 것과 같은 방식입니다.
    // 숫자를 넣어 저장하면 그게 관계입니다.
    @Transactional
    public PostResponseDto createReply(
            PostRequestDto dto,
            Long parentId,
            Long userId){
        // 부모글 있는지 확인
        Post parent = postRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        // 있다면 답글 작성
        Post post = Post.builder()
                .rootId((parent.getRootId() != null) ? parent.getRootId() : parent.getId())
                //parentId의 sonId 관계 설정
                .parentId(parentId)
                .userId(userId)
                .content(dto.getContent())
                .imageUrl(dto.getImageUrl())
                .build();

        //답글 증가
        postRepository.increaseComment(parentId);
        //세이브(답글로)
        postRepository.save(post);

        return PostResponseDto.from(post);
    }
    // 댓글과 글의 관계

    // 댓글 목록(마찬가지로 게시판의 댓글을 보는 기능이 아니라, X마냥 피드의 답글을 보는 개념)
    // 루트 id
    @Transactional(readOnly = true)
    public List<PostResponseDto> getReplys(Long id){
        return postRepository.findByParentIdOrderByCreatedAtAsc(id)
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }
    //C
    @Transactional
    public PostResponseDto createPostItem(
            PostRequestDto dto,
            Long userId){
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
    public PostResponseDto updatePostItem(
            Long id,
            Long userId,
            PostRequestDto dto){
        Post post = postRepository.findById(id)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.update(userId, dto.getContent(), dto.getImageUrl());
        return PostResponseDto.from(post);
    }
    //D
    //soft delete로 방향 수정
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
        // (entity에서 조건을 바꿔놔서 update가 실제로 진행)
        postRepository.delete(post);
        //원글이 아닐때
        if(post.getParentId() != null){
            postRepository.decreaseComment(post.getParentId());
        }
        // 삭제된 게시글 정보를 DTO로 변환하여 반환
        return PostResponseDto.from(post);
    }

    // 해당 포스트가 존재하는지 탐색, MSA으로 추후에 전환할것이기에 임시
    @Transactional(readOnly = true)
    public Boolean existByPost(Long id){
        return postRepository.existsById(id);
    }
}
