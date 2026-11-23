package com.example.login.profile;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.follow.*;
import com.example.login.user.Users;
import com.example.login.user.UsersRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UsersRepository usersRepository;
    private final FollowRepository followRepository;


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
    public FollowPageResponseDto getFollowerItem(
            Long userId,
            Long targetUserId,
            Long cursor,
            int size) {
        int pageSize = Math.min(Math.max(size, 1), 50);
        // getFeed와 비슷한 설계.
        // Cache로 받아오는게 아니라, DB에서 받아옴


        Limit limit = Limit.of(pageSize + 1);
        List<Follow> fetchedIds = (cursor == null)
                ? followRepository.findByFollowingIdOrderByIdDesc(targetUserId, limit)
                : followRepository.findByFollowingIdAndIdLessThanOrderByIdDesc(targetUserId, cursor, limit);
        boolean hasNext = fetchedIds.size() > pageSize;
        List<Follow> targetFollows = hasNext ? fetchedIds.subList(0, pageSize) : fetchedIds;
       // 타겟이 없다면 빈걸 리턴한다.
        if(targetFollows.isEmpty()){
            return new FollowPageResponseDto(List.of(), null);
        }
        // 잘라낸 Follow에서 팔로워의 사용자 ID만 추출 후 Users 일괄 조회 및 순서 복원
        List<Long> userIds = targetFollows.stream()
                .map(Follow::getFollowerId)
                .toList();
        Map<Long, Users> userMap = usersRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(Users::getId, Function.identity()));

        List<Users> orderedUsers = userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .toList();
        Long nextCursor = hasNext ? targetFollows.get(targetFollows.size() - 1).getId() : null;

        // 현재 로그인한 유저(userId)가 이 사람들을 팔로우 중인지 Set으로 추출
        Set<Long> followedSet = (userId == null)
                ? Set.of()
                : followRepository.findByFollowerIdAndFollowingIdIn(userId, userIds)
                .stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toSet());
        // DTO 변환 및 반환
        List<FollowUserResponseDto> items = orderedUsers.stream()
                .map(user -> FollowUserResponseDto.from(user, followedSet.contains(user.getId())))
                .toList();
        return new FollowPageResponseDto(items, nextCursor);
    }
    // 팔로잉 반환
    @Transactional(readOnly= true)
    public FollowPageResponseDto getFollowingItem(
            Long followerId,
            Long cursor,
            int size) {
        int pageSize = Math.min(Math.max(size, 1), 50);
        Limit limit = Limit.of(pageSize + 1);
        List<Follow> fetchedIds = (cursor == null)
                ? followRepository.findByFollowerIdOrderByIdDesc(followerId, limit)
                : followRepository.findByFollowerIdAndIdLessThanOrderByIdDesc(followerId, cursor, limit);
        boolean hasNext = fetchedIds.size() > pageSize;
        List<Follow> targetFollows = hasNext ? fetchedIds.subList(0, pageSize) : fetchedIds;
        // 타겟 아이ㅣ 없다면 빈걸 리턴한다.
        if(targetFollows.isEmpty()){
            return new FollowPageResponseDto(List.of(), null);
        }
        // 잘라낸 Follow에서 팔로워의 사용자 ID만 추출 후 Users 일괄 조회 및 순서 복원
        List<Long> userIds = targetFollows.stream()
                .map(Follow::getFollowingId)
                .toList();
        Map<Long, Users> userMap = usersRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(Users::getId, Function.identity()));

        List<Users> orderedUsers = userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .toList();
        Long nextCursor = hasNext ? targetFollows.get(targetFollows.size() - 1).getId() : null;

        // DTO 변환 및 반환
        List<FollowUserResponseDto> items = orderedUsers.stream()
                .map(user -> FollowUserResponseDto.from(user, true))
                .toList();
        return new FollowPageResponseDto(items, nextCursor);
    }



    private Long getFollowingCount(Users user){
        return followRepository.countByFollowerId(user.getId());
    }

    private Long getFollowerCount(Users user){
        return followRepository.countByFollowingId(user.getId());
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
