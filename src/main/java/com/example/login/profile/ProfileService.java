package com.example.login.profile;

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
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 정보를 찾을 수 없습니다."));

        return ProfileResponseDto.from(user,
                viewerId,
                postRepository,
                followRepository
        );
    }

    @Transactional
    public void updateMyProfile(ProfileRequestDto dto, Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("바꿀수 있는 계정 정보가 존재하지 않습니다."));
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
}
