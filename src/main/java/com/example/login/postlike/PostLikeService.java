package com.example.login.postlike;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.post.PostRepository;
import com.example.login.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    @Transactional
    public boolean toggle(Long postId, Long userId){
        // 포스트 없으면 에러 반환
        if(postId == null || !postService.existByPost(postId)){
            throw new BusinessException((ErrorCode.POST_NOT_FOUND));
        }
        // 이미 좋아요를 한 상태에서 누른다면 좋아요를 취소함(모달같은거 안띄움)
        // 아니면 좋아요 눌려짐
        // 저장 성공시
        //eventPublisher.publishEvent(new PostLikeEvent(id, postId);
        return postLikeRepository.findByPostIdAndUserId(postId, userId)
                .map(postLike -> {
                    postLikeRepository.delete(postLike);
                    postRepository.decreaseLike(postId);
                    return false;
                })
                .orElseGet(()->{
                    postLikeRepository.save(new PostLike(postId, userId));
                    postRepository.increaseLike(postId);
                    return true;
                });
    }


    @Transactional(readOnly = true)
    public List<Long> getPostLikeService(Long postId){
        return postLikeRepository.findByPostId(postId);
    }
}
