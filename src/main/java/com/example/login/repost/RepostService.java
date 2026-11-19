package com.example.login.repost;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.event.EventMessage;
import com.example.login.post.Post;
import com.example.login.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// 리포스트 수정의 개념은, 게시글 수정으로 넘겨도 될듯 하다.
@Service
@RequiredArgsConstructor
public class RepostService {
    private final RepostRepository repostRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;
    //리포스트 글 조회?(이건 게시글로 되지 않을까?)


    // 리포스트
    @Transactional
    public RepostResponseDto repost(
            Long userId,
            Long postId
    ){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Optional<Repost> existing = repostRepository.findByUserIdAndPostId(userId, postId);
        boolean reposted;
        if(existing.isPresent()){
            repostRepository.delete(existing.get());
            postRepository.decreaseRepostCount(postId);
            reposted = false;
        }
        else{
            repostRepository.save(new Repost(userId, postId));
            postRepository.increaseRepostCount(postId);
            eventPublisher.publishEvent(
                    EventMessage.ofRepost(
                            userId,
                            postId,
                            post.getUserId()
                    )
                );
            reposted = true;
        }
        int currentRepostCount = postRepository.findRepostCountById(postId);
        return RepostResponseDto.from(reposted, currentRepostCount);
    }

}
