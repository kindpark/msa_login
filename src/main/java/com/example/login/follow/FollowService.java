package com.example.login.follow;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.user.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UsersService usersService;

    @Transactional(readOnly = true)
    public List<Long> getMyFollowings(Long userId){
        return followRepository.findFollowingIds(userId);
    }

    @Transactional
    public boolean toggle(Long userId, Long targetUserId){
        if(userId.equals(targetUserId)){
            // 셀프 팔로우 불가
            throw new BusinessException(ErrorCode.SELF_FOLLOW);
        }
        if(targetUserId == null || !usersService.existsById(targetUserId)){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 이미 팔로우 중이면 삭제하고 false 반환
        // 아니면 저장
        // 저장 성공시
        //eventPublisher.publishEvent(new FollowCreatedEvent(userId, myNickname, targetUserId));
        return followRepository.findByFollowerIdAndFollowingId(userId, targetUserId)
                .map(follow -> {
                    //이미 있으면 삭제
                    followRepository.delete(follow);
                    return false;
                })
                .orElseGet(() -> {
                    followRepository.save(new Follow(userId, targetUserId));
                    return true;
                });
    }
}
