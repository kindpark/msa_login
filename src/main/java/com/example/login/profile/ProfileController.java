package com.example.login.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<ProfileResponseDto> getProfileHeader(
            @AuthenticationPrincipal Long id
    ){
        return ResponseEntity.ok(profileService.getProfileHeaderItem(id));
    }

    //
    @PatchMapping("/users/me")
    public ResponseEntity<ProfileResponseDto> updateProfileheader(
        @AuthenticationPrincipal Long id,
        @RequestBody ProfileRequestDto dto,

    ){

    }
}
