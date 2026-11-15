package com.example.login.profile;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.follow.FollowRepository;
import com.example.login.follow.FollowResponseDto;
import com.example.login.follow.FollowsInfo;
import com.example.login.post.PostRepository;
import com.example.login.post.PostResponseDto;
import com.example.login.user.UserResponseDto;
import com.example.login.user.Users;
import com.example.login.user.UsersRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UsersRepository usersRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;


    // 프로필 헤더들을 반환하기 위한것
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(Long targetUserId, Long viewerId) {
        Users user = usersRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return ProfileResponseDto.from(
                user,
                viewerId,
                getFollowerCount(user),
                getFollowingCount(user),
                getPostCount(user),
                getMe(user, viewerId),
                getFollowedByMe(user, viewerId)
        );
    }

    @Transactional
    public void updateMyProfile(ProfileRequestDto dto, Long userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if(dto.getHandle() != null
                && !Objects.equals(dto.getHandle(), user.getHandle())
                && usersRepository.existsByHandle(dto.getHandle())){
            throw new BusinessException(ErrorCode.DUPLICATE_HANDLE);
        }
        user.updateUsersInfo(
                dto.getNickname(),
                dto.getProfileImageUrl(),
                dto.getBio(),
                dto.getHandle(),
                dto.getBannerImageUrl()
        );
    }
    //팔로워 반환
    @Transactional(readOnly = true)
    public List<FollowResponseDto> getFollowers(
            Long targetUserId,
            ProfileRequestDto dto) {
        List<Long> followerIds = followRepository.findFollowerIds(targetUserId);

        //유저 엔티티 조회
        List<Users> followers = usersRepository.findAllById(followerIds);

        return followers.stream()
                .map(FollowResponseDto::from)
                .toList();
    }
    // 팔로잉 반환
    @Transactional(readOnly= true)
    public ProfileResponseDto getFollowings(
            Long targetUserId,
            ProfileRequestDto dto) {

        List<Long> followingIds = followRepository.findFollowingIds(targetUserId);
        //유저 엔티티 조회
        List<Users> followings = usersRepository.findAllById(followingIds);

        return followings.stream()
                .map(FollowResponseDto::from)
                .toList();
    }



    private Long getFollowingCount(Users user){
        return followRepository.countByFollowerId(user.getId());
    }

    private Long getFollowerCount(Users user){
        return followRepository.countByFollowingId(user.getId());
    }

    private Long getPostCount(Users user) {
        return postRepository.countByUserIdAndParentIdIsNull(user.getId());
    }

    private Boolean getMe(Users user, Long viewerId) {
        return Objects.equals(viewerId, user.getId());
    }

    private Boolean getFollowedByMe(Users user, Long viewerId) {
        boolean me = Objects.equals(viewerId, user.getId());
        return me ? null : viewerId != null
                && followRepository.existsByFollowerIdAndFollowingId(viewerId, user.getId());
    }
}
