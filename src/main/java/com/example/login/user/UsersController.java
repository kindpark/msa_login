package com.example.login.user;


import com.example.login.auth.AuthService;
import com.example.login.user.SignupRequestDto;
import com.example.login.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {
    private final UsersService usersService;
    // 1. 회원가입 API (누락되었던 부분 추가)
    @PostMapping("/signup")
    public ResponseEntity<String> signup(
            @Valid @RequestBody SignupRequestDto request) {
        usersService.signUp(request);
        return ResponseEntity.ok("회원가입 완료");
    }

    // 본인 정보 확인
    @GetMapping("/me")
    public ResponseEntity<UserMeResponseDto> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(usersService.getMyInfo(userId));
    }
}
