package com.example.login.profile;

import com.example.login.user.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<List<UserResponseDto>> getProfileHeader(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(profileService.getProfileHeaderItem(userId));
    }

    //
    @PatchMapping("/users/me")
    public ResponseEntity<Void> fetchProfileHeader(
        @RequestBody ProfileRequestDto dto,
        @AuthenticationPrincipal Long userId
    ){
        profileService.patchProfileHeader(dto, userId);
        return ResponseEntity.noContent().build();
    }
    // 탭 목록들
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<List<UserResponseDto>> listTap(Long userId){
        return ResponseEntity.ok(profileService.getListTapItem(userId));
    }
    // 팔로워
    @GetMapping("/users/{userId}/followers")
    public ResponseEntity<ProfileResponseDto> followerList(
            @AuthenticationPrincipal Long userId,
            @RequestBody ProfileRequestDto dto
    ){
        return ResponseEntity.ok(profileService.followerListItem(userId, dto));
    }
    // 팔로잉
    @GetMapping("/users/{userId}/followings")
    public ResponseEntity<ProfileResponseDto> followingList(
            @AuthenticationPrincipal Long userId,
            @RequestBody ProfileRequestDto dto
    ){
        return ResponseEntity.ok(profileService.followingListItem(userId, dto));
    }
}
