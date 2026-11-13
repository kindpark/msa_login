package com.example.login.profile;

import com.example.login.follow.Follow;
import com.example.login.follow.FollowRepository;
import com.example.login.post.PostRepository;
import com.example.login.user.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponseDto {
    private Long id;
    private String handle;
    private String nickname;
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
    private LocalDateTime createdAt;
    private Long followerCount;
    private Long followingCount;
    private Long postCount;
    private Boolean me;
    private Boolean followedByMe;
    // MSA로 나누면 PostCount를 이벤트로 받도록 한다.
    public static ProfileResponseDto from(Users user, Long viewerId, PostRepository postRepository, FollowRepository followRepository){
        boolean me = Objects.equals(viewerId, user.getId());
        return ProfileResponseDto.builder()
                .id(user.getId())
                .handle(user.getHandle())
                .nickname(user.getNickname())
                .bio(user.getBio())
                .profileImageUrl(user.getProfileImageUrl())
                .bannerImageUrl(user.getBannerImageUrl())
                .createdAt(user.getCreatedAt())
                .followerCount(followRepository.countByFollowingId(user.getId()))
                .followingCount(followRepository.countByFollowerId(user.getId()))
                //잘 몰랐던 부분들. me와 followedByMe를 왜 이렇게 받아야 하는가?
                .postCount(postRepository.countByUserIdAndParentIdIsNull(user.getId()))
                .me(me)
                .followedByMe(me ? null : viewerId != null && followRepository.existsByFollowerIdAndFollowingId(viewerId, user.getId()))
                .build();
    }
}
