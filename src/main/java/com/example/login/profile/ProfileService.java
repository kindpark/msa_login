package com.example.login.profile;

import com.example.login.follow.FollowRepository;
import com.example.login.follow.FollowResponseDto;
import com.example.login.follow.FollowsInfo;
import com.example.login.user.UserResponseDto;
import com.example.login.user.Users;
import com.example.login.user.UsersInfo;
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

    // 프로필 헤더들을 반환하기 위한것
    @Transactional(readOnly = true)
    public List<UserResponseDto> getProfileHeaderItem(Long id, Long userId) {
        // 임시 JPA
        List<UsersInfo> user = usersRepository.findById(id);

        return user.stream()
                .map(users -> UserResponseDto.from(users.getUser()))
                .toList();
    }

    @Transactional
    public void patchProfileHeader(ProfileRequestDto dto, Long userId) {
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
    // 탭 목록 반환을 위한것
    @Transactional(readOnly = true)
    public List<UserResponseDto> getListTapItem() {
        //임시 JPA
        List<UsersInfo> user = usersRepository.findById(id);

        return user.stream()
                .map(users -> UserResponseDto.from(users.getUser()))
                .toList();
    }
    //팔로워 반환
    @Transactional(readOnly = true)
    public ProfileResponseDto followerListItem(Long userId, ProfileRequestDto dto) {
        // 임시 JPA
        List<FollowsInfo> follow = followRepository.findById();

        return follow.stream()
                .map(follows -> FollowResponseDto.from())
                .toList();
    }
    // 팔로잉 반환
    @Transactional(readOnly= true)
    public ProfileResponseDto followingListItem(Long userId, ProfileRequestDto dto) {
    }
}
