package com.example.login.follow;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.event.EventMessage;
import com.example.login.user.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UsersService usersService;
    private final ApplicationEventPublisher eventPublisher;
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
        return followRepository.findByFollowerIdAndFollowingId(userId, targetUserId)
                .map(follow -> {
                    //이미 있으면 삭제
                    followRepository.delete(follow);
                    return false;
                })
                .orElseGet(() -> {
                    // 닉네임은 넣지 않는다. 닉네임으로 설정하면
                    // 닉네임 바꾼다고 가정했을때, 닉네임 바꾸기 전 전송된 알림에는 계속 옛날 닉네임이
                    // 남아 혼동을 초래할수 있다. 아이디로 관리하면 닉네임이 아이디에 연동되므로 자동 변경
                    // 그리고 MSA에 위배된다.
                    followRepository.save(new Follow(userId, targetUserId));
                    eventPublisher.publishEvent(EventMessage.ofFollow(userId, targetUserId));
                    return true;
                });
    }
}
