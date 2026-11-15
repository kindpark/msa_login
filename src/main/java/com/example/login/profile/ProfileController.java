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

    @GetMapping("/users/{targetUserId}/profile")
    public ResponseEntity<ProfileResponseDto> getProfile(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(profileService.getProfile(targetUserId, userId));
    }

    //
    @PatchMapping("/users/me")
    public ResponseEntity<Void> updateMyProfile(
        @RequestBody ProfileRequestDto dto,
        @AuthenticationPrincipal Long userId
    ){
        profileService.updateMyProfile(dto, userId);
        return ResponseEntity.noContent().build();
    }
    // 팔로워
    @GetMapping("/users/{targetUserId}/followers")
    public ResponseEntity<ProfileResponseDto> getFollowers(
            @AuthenticationPrincipal Long userId,
            @RequestBody ProfileRequestDto dto
    ){
        return ResponseEntity.ok(profileService.getFollowers(userId, dto));
    }
    // 팔로잉
    @GetMapping("/users/{targetUserId}/followings")
    public ResponseEntity<ProfileResponseDto> getFollowings(
            @AuthenticationPrincipal Long userId,
            @RequestBody ProfileRequestDto dto
    ){
        return ResponseEntity.ok(profileService.getFollowings(userId, dto));
    }
}
