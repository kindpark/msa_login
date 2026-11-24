package com.example.login.profile.controller;

import com.example.login.follow.dto.FollowPageResponseDto;
import com.example.login.profile.dto.ProfileRequestDto;
import com.example.login.profile.dto.ProfileResponseDto;
import com.example.login.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        @RequestBody @Valid ProfileRequestDto dto,
        @AuthenticationPrincipal Long userId
    ){
        profileService.updateMyProfile(dto, userId);
        return ResponseEntity.noContent().build();
    }
    // 팔로워
    @GetMapping("/users/{targetUserId}/followers")
    public ResponseEntity<FollowPageResponseDto> getFollowers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long targetUserId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(profileService.getFollowerItem(userId, targetUserId, cursor, size));
    }
    // 팔로잉
    @GetMapping("/users/me/followings")
    public ResponseEntity<FollowPageResponseDto> getFollowings(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(profileService.getFollowingItem(userId, cursor, size));
    }
}
